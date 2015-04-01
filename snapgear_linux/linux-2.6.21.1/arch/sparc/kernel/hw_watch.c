#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/proc_fs.h>
#include <linux/string.h>
#include <linux/ioport.h>
#include <linux/io.h>

#include <asm/traps.h>

#include <asm/uaccess.h>

#include "simulator.h"

#define WP_ADDR_MASK 0xfffffffc
#define WP_MASK_MASK 0xfffffffc

#define WP_ADDR_BP_MASK 0x1
#define WP_ADDR_IMP_MASK 0x2

#define WP_MASK_DSTORE_MASK 0x1
#define WP_MASK_DLOAD_MASK 0x2

int PRINT_INST = 0;

struct watchpoint {
    u32 addr;
    u32 mask;
} __attribute__((__packed__));

#define MAX_NUM_WP 4
#define HW_WATCH_MAX_LEN (MAX_NUM_WP * sizeof(struct watchpoint))

static struct watchpoint wpList[MAX_NUM_WP];

static struct file_operations hw_watch_fops;
static int hw_watch_major = 0;
DECLARE_WAIT_QUEUE_HEAD(hw_watch_queue);

// shared variables, must be locked
#define MAX_STORED_WP 1024
static u32 wp_buffer[MAX_STORED_WP];
static volatile u32 wp_buffer_head = 0;
static volatile u32 wp_buffer_tail = 0;

#define IMP_WP_VAL 0x6f6c6568

//static volatile u32 wp_miss_count = 0;

//static unsigned long false_pos = 0;

static void asmsetasr24(u32 a) { asm(" wr %0, %%asr24" : : "r"(a));}
static void asmsetasr25(u32 a) { asm(" wr %0, %%asr25" : : "r"(a));}
static u32 asmgetasr24(void) { u32 a; asm(" rd %%asr24, %0" : "=r"(a)); return a;}
static u32 asmgetasr25(void) { u32 a; asm(" rd %%asr25, %0" : "=r"(a)); return a;}

static void asmsetasr26(u32 a) { asm(" wr %0, %%asr26" : : "r"(a));}
static void asmsetasr27(u32 a) { asm(" wr %0, %%asr27" : : "r"(a));}
static u32 asmgetasr26(void) { u32 a; asm(" rd %%asr26, %0" : "=r"(a)); return a;}
static u32 asmgetasr27(void) { u32 a; asm(" rd %%asr27, %0" : "=r"(a)); return a;}

static void asmsetasr28(u32 a) { asm(" wr %0, %%asr28" : : "r"(a));}
static void asmsetasr29(u32 a) { asm(" wr %0, %%asr29" : : "r"(a));}
static u32 asmgetasr28(void) { u32 a; asm(" rd %%asr28, %0" : "=r"(a)); return a;}
static u32 asmgetasr29(void) { u32 a; asm(" rd %%asr29, %0" : "=r"(a)); return a;}

static void asmsetasr30(u32 a) { asm(" wr %0, %%asr30" : : "r"(a));}
static void asmsetasr31(u32 a) { asm(" wr %0, %%asr31" : : "r"(a));}
static u32 asmgetasr30(void) { u32 a; asm(" rd %%asr30, %0" : "=r"(a)); return a;}
static u32 asmgetasr31(void) { u32 a; asm(" rd %%asr31, %0" : "=r"(a)); return a;}

/*static void wsysreg(u32 addr, u32 data) {asm("sta %1, [%0] 0x2" : : "r"(addr), "r"(data));}
  static u32 rsysreg(u32 addr) { u32 a; asm(" lda [%1] 0x2, %0" : "=r"(a) : "r"(addr)); return a;} */

#define CCTRL_IFP (1<<15)
#define CCTRL_DFP (1<<14)

#define DSU_CONTROL_ADDRESS 0x90000000

static int numInstEmu = 0;
static int numDceFaults = 0;
static int numKernFaults = 0;

/**************************** helper functions for the simulator ***************/
void unknownInst(struct CPU *cpu, struct control_signals *control) {
    printk(KERN_CRIT "op = 0x%x\n", control->op);
    printk(KERN_CRIT "op2 = 0x%x\n", control->op2);
    printk(KERN_CRIT "op3 = 0x%x\n", control->op3);
    printk(KERN_CRIT "pc = 0x%x\n", cpu->pc);
    assert(false);
}


static void flush_windows(void) {
        synchronize_user_stack();
}

static u32 my_get_user_u32(u32 *addr) {
        u32 ret = 0xdeadbeef;
        int r = get_user(ret, addr);

        if(r != 0) {
                panic("could not get user addr");
        }

        return ret;
}

static void my_put_user_u32(u32 *addr, u32 value) {
        int r = put_user(value, addr);
        if(r != 0) {
                panic("could not set user addr");
        }
}

bool guestLoad(u32 guestAddr, u32 *value) {
        int ret;
        if(guestAddr & 0x3) {
                printk(KERN_CRIT "unaligned guest load\n");
                return -1;
        }
        ret = get_user(*value, (u32 *) guestAddr);
        return ret == 0;
}

bool guestStore(u32 guestAddr, u32 value) {
        int ret;
        if(guestAddr & 0x3) {
                printk(KERN_CRIT "unaligned guest store\n");
                return -1;
        }
        ret = put_user(value, (u32 *) guestAddr);
        return ret == 0;
}

static u32 fetch_reg(u32 reg, struct pt_regs *regs) {
    struct reg_window *win = (struct reg_window __user *) regs->u_regs[14];
    u32 ret = 0xdeadbeef;
    if(reg < 16) {
            ret = regs->u_regs[reg];
    } else if (reg < 24) {
            ret = my_get_user_u32(&(win->locals[reg-16]));
    } else {
            ret = my_get_user_u32(&(win->ins[reg-24]));
    }

    return ret;
}

static void store_reg(u32 value, u32 reg, struct pt_regs *regs) {
    struct reg_window *win = (struct reg_window __user *) regs->u_regs[14];
    if(reg < 16) {
            regs->u_regs[reg] = value;
    } else if (reg < 24) {
            my_put_user_u32(&(win->locals[reg-16]), value);
    } else {
            my_put_user_u32(&(win->ins[reg-24]), value);
    }
}

/*******************************************************************************/

static int dsu_disabled = 0;

static void disable_dsu_watchpoints(void) {
    unsigned long * dsu_control;

    if (dsu_disabled != 0)
	return;
    
    dsu_control = ioremap(DSU_CONTROL_ADDRESS,4);
    
    *dsu_control &= ~(1<<2);

    dsu_disabled = 1;
}

static void read_wp(struct watchpoint *wp) {
        wp[0].addr = asmgetasr24();
        wp[0].mask = asmgetasr25();

        wp[1].addr = asmgetasr26();
        wp[1].mask = asmgetasr27();

        wp[2].addr = asmgetasr28();
        wp[2].mask = asmgetasr29();

        wp[3].addr = asmgetasr30();
        wp[3].mask = asmgetasr31();

        //print_cache();
}

static void write_wp(struct watchpoint *wp) {
        asmsetasr24(wp[0].addr);
        asmsetasr25(wp[0].mask);

        asmsetasr26(wp[1].addr);
        asmsetasr27(wp[1].mask);

        asmsetasr28(wp[2].addr);
        asmsetasr29(wp[2].mask);

        asmsetasr30(wp[3].addr);
        asmsetasr31(wp[3].mask);

        // burn some cycles
        asm volatile("nop; nop; nop");
}


// XXX FIXME, figure out how read_proc functions are supposed to actually work
static int hw_watch_read_proc(char *buf, char **start, off_t offset, int count,
			 int *eof, void *data)
{
    /*
    printk(KERN_CRIT "number of DCE faults = %d\n", numDceFaults);
    printk(KERN_CRIT "number of inst emul  = %d\n", numInstEmu);
    numDceFaults = 0;
    numInstEmu = 0;    

    return 0;
    */
    PRINT_INST = 1;
    int len = sprintf(buf, "%d %d %d\n", numDceFaults, numKernFaults, numInstEmu);
    numDceFaults = 0;
    numKernFaults = 0;
    numInstEmu = 0;
    return len;
}

static int wp_hit(u32 addr, u32 mask, u32 value) {
        u32 match;

        match = value;
        match ^= addr;
        match &= mask;

        return match == 0;
}

static int valid_load_store(u32 addr, u32 mask) {
        int valid = 1;
        u32 idx;

        for(idx = 0; idx < MAX_NUM_WP; idx++) {
                if(wp_hit(addr, mask, (u32) &(wpList[idx].addr)) ||
                   wp_hit(addr, mask, (u32) &(wpList[idx].mask))) {
                        valid = 0;
                }
        }

        return valid;
}

static int valid_imp(u32 addr, u32 mask, u32 idx) {
        int valid = 1;
        u32 i;

        // values are written to hardware in two passes.  The first
        // pass sets the addr and mask WITHOUT activating any 
        // watchpoints.  The second pass activates the watchpoints (with
        // the mask already in place) so we are looking to see if any
        // values beyond the current, in the second pass, will cause
        // a trap

        for(i = idx; i < MAX_NUM_WP; i++) {
                if(wp_hit(addr, mask, wpList[i].mask)) {
                        valid = 0;
                }
                if((i != idx) && wp_hit(addr, mask, wpList[i].addr)) {
                        valid = 0;
                }
        }

        if(!valid) {
                printk(KERN_CRIT "invalid imp wp\n");
        }

        return valid;
}

// XXX FIXME -- make sure we do not have a breakpoint set in this module
static int valid_bp(u32 addr, u32 mask) {
        return 1;
}

static int valid_wp(struct watchpoint *wp, u32 idx) {
        int valid = 1;
        u32 addr, mask;

        addr = wp->addr & WP_ADDR_MASK;
        mask = wp->mask & WP_MASK_MASK;

        if(wp->addr & WP_ADDR_BP_MASK) {
                if(!valid_bp(addr, mask)) {
                        valid = 0;
                }
        }

        if(wp->addr & WP_ADDR_IMP_MASK) {
                if(!valid_imp(addr, mask, idx)) {
                        valid = 0;
                }
        }

        if(wp->mask & (WP_MASK_DLOAD_MASK | WP_MASK_DSTORE_MASK)) {
                if(!valid_load_store(addr, mask)) {
                        valid = 0;
                }
        }

        if(!valid) {
                printk(KERN_CRIT "squashing wp\n");
        }

        return valid;
}

static void write_wp_list(struct watchpoint *dstWp, struct watchpoint *srcWp) {
        unsigned int idx;

        for(idx = 0; idx < MAX_NUM_WP; idx++) {
                if(!valid_wp(srcWp+idx, idx)) {
                        srcWp[idx].addr = 0;
                        srcWp[idx].mask = 0;
                }
                // do not enable, setup addresses and masks first
                // to avoid spurious breakpoints
                dstWp[idx].addr = srcWp[idx].addr & WP_ADDR_MASK;
                dstWp[idx].mask = srcWp[idx].mask & WP_MASK_MASK;
        }
        write_wp(dstWp);

        // now enable them
        for(idx = 0; idx < MAX_NUM_WP; idx++) {
                dstWp[idx].addr = srcWp[idx].addr;
                dstWp[idx].mask = srcWp[idx].mask;
        }
        write_wp(dstWp);
}        

static int hw_watch_write_proc(struct file *file, const char __user *buffer,
			  unsigned long count, void *data)
{
        struct watchpoint *wp;
        u32 idx;
	int i;
	
	void* new_addr;

	printk(KERN_CRIT "hw_watch_write_proc executed\n");

        wp = kmalloc(sizeof(struct watchpoint) * MAX_NUM_WP, GFP_KERNEL);
        if(wp == NULL) {
                return -ENOMEM;
        }

        // clear current watchpoints
        for(idx = 0; idx < MAX_NUM_WP; idx++) {
                wp[idx].addr = 0;
                wp[idx].mask = 0;
        }

	if (count == 9) {
	    char pointer_buf[10];
	    
	    if (copy_from_user(pointer_buf, buffer, 9)) {
		kfree(wp);
		return -EFAULT;
	    }
	    
	    pointer_buf[9] = '\0';
	    
	    new_addr = simple_strtol(pointer_buf, NULL, 16);
	    
	    printk(KERN_CRIT "Requested Watchpoint Address: 0x%p\n", new_addr);
	    
	    wp[1].addr = (unsigned long)new_addr | 0x00000001; /* Specify it as an instruction fetch watchpoint */
	    wp[1].mask = (unsigned long)0xFFFFFFFF & 0xFFFFFFFC;
		    
	} else if (count == 4) {
	    if (copy_from_user(&new_addr, buffer, 4)) {
		kfree(wp);
		return -EFAULT;
	    }
	    
	    printk(KERN_CRIT "Requested Watchpoint Address: 0x%p\n", new_addr);
	    
	    wp[1].addr = (unsigned long)new_addr | 0x00000001; /* Specify it as an instruction fetch watchpoint */
	    wp[1].mask = (unsigned long)0xFFFFFFFF & 0xFFFFFFFC;
        }

        //write_wp_list(wpList, wp);
        write_wp(wp);
        kfree(wp);

	return count;
}

static int in_watch = 0;
static u32 lastPc = 0;
static u32 numInst = 0;

void handle_watchpoint(struct pt_regs *regs, unsigned long pc, unsigned long npc,
                       unsigned long psr)
{
        int idx;
        struct CPU *cpu;
        bool ret;
        u32 sp, new_CWP;

        numDceFaults++;

        myprintf("handle watchpoint, pc = 0x%08lx, pc = 0x%08lx\n", regs->pc, pc);

        // XXX FIXME yes, I know we should disable interrupts here
        if(in_watch) {
            in_watch++;
            if(in_watch > 10) {
                panic("recursive dce fault at pc = 0x%08lx\n", regs->pc);
            }
            return;
        }
        in_watch = 1;

        myprintf("handle_watchpoint executed at pc = 0x%08lx\n", regs->pc);
        for(idx = 0; idx < MAX_NUM_WP; idx++) {
                wpList[idx].addr = 0;
                wpList[idx].mask = 0;
        }
        write_wp(wpList);
        
        //if(lastPc == regs->pc) {
            lastPc = 0;
            numInst = 1;            
        /*} else {
            lastPc = regs->pc;
            numInst = 0;
        }*/

        if(numInst == 0) {
            in_watch = 0;
            return;
        }

        if(!user_mode(regs)) {
                numKernFaults++;
                myprintf("trap and return for kernel dce at 0x%08x\n", regs->pc);
                in_watch = 0;
                return;
        }

        flush_windows();

        cpu = kmalloc(sizeof(struct CPU), GFP_KERNEL);
        memset(cpu, 0, sizeof(struct CPU));
        if(cpu == NULL) {
                panic("could not alloc CPU struct");
        }

        cpu->pc = regs->pc;
        cpu->npc = regs->npc;
        cpu->psr = regs->psr;
        cpu->y = regs->y;
        cpu->tbr = get_tbr();
        cpu->branch_pc = (u32) -1;

        // XXX FIXME assume 8 register windows
        cpu->wim = 0x1 << (cpu->psr & 0x1f);
        if(cpu->wim == 0x80) {
            cpu->wim = 0x1;
        } else {
            cpu->wim <<= 1;
        }

        // this is only for the current window
        for(idx = 0; idx < 32; idx++) {                
                set_reg(cpu, idx, fetch_reg(idx, regs));
        }

        for(idx = 0; idx < numInst; idx++) {
                if(!cpu_exec_inst(cpu)) {
                        break;
                }
                numInstEmu++;
        }

        // Note: I am not storing the tbr back since I am going to let usermode simply
        // execute the instruction that causes the fault so the state will be
        // reproduced and if we want to set it we should emulate the hardware trap mechanisms
        // and setup the register appropriately
        regs->pc = cpu->pc;
        regs->npc = cpu->npc;
        regs->psr = (cpu->psr & 0xffffffe0) | (regs->psr & 0x0000001f);
        regs->y = cpu->y;
        for(idx = 0; idx < 32; idx++) {
            store_reg(get_reg(cpu, idx), idx, regs);
        }
        
        new_CWP = (CWP(cpu)+1) % NUM_WINDOWS;
        while(!(cpu->wim & (1 << new_CWP))) {
            myprintf("spilling stack for CWP %u\n", new_CWP);
            cpu->psr &= ~(0x1f);
            cpu->psr |= new_CWP;
            sp = get_reg(cpu, SP);
            
            ret = guestStore(sp + 0, get_reg(cpu, L0)); assert(ret);
            ret = guestStore(sp + 4, get_reg(cpu, L1)); assert(ret);
            ret = guestStore(sp + 8, get_reg(cpu, L2)); assert(ret);
            ret = guestStore(sp + 12, get_reg(cpu, L3)); assert(ret);
            ret = guestStore(sp + 16, get_reg(cpu, L4)); assert(ret);
            ret = guestStore(sp + 20, get_reg(cpu, L5)); assert(ret);
            ret = guestStore(sp + 24, get_reg(cpu, L6)); assert(ret);
            ret = guestStore(sp + 28, get_reg(cpu, L7)); assert(ret);
            
            ret = guestStore(sp + 32, get_reg(cpu, I0)); assert(ret);
            ret = guestStore(sp + 36, get_reg(cpu, I1)); assert(ret);
            ret = guestStore(sp + 40, get_reg(cpu, I2)); assert(ret);
            ret = guestStore(sp + 44, get_reg(cpu, I3)); assert(ret);
            ret = guestStore(sp + 48, get_reg(cpu, I4)); assert(ret);
            ret = guestStore(sp + 52, get_reg(cpu, I5)); assert(ret);
            ret = guestStore(sp + 56, get_reg(cpu, I6)); assert(ret);
            ret = guestStore(sp + 60, get_reg(cpu, I7)); assert(ret);
            
            new_CWP = (CWP(cpu)+1) % NUM_WINDOWS;
        }

        if(cpu->pending_trap) {
            printk(KERN_CRIT "got trap (tt = 0x%x), return to user and let kern handle\n", 
                   cpu->pending_trap);
            cpu->pending_trap = 0;
        }

        kfree(cpu);

        in_watch = 0;
}

/*
 * if we wanted to do this in the kernel:
 * 
 * 1) on trap we need to store the register file in memory
 *    a) to avoid kernel stack overflows, alloc a CPU struct on the side
 *    b) clear out the WIM so we can access all register windows
 *    c) store the globals
 *    d) store the locals and ins for each of the windows
 *    e) store all of the privileged registers
 *
 * 2) jump to the simulator.  Note: it must be compiled with mflat!!!!!
 *    a) also, we need to make sure we are NOT calling any external funcs
 *    b) we need an alternative load/store implemenation for kernel that walks
 *       page tables and such????
 *
 * 3) restore the state (opposite of 1)
 *
 * 4) rett directly without going through the kernel stuff
 */


static struct proc_dir_entry *hw_watch;

#define HW_WATCH_VERSION	"0.3"


static int hw_watch_open(struct inode *inode, struct file *file) {
	return 0;
}

static int hw_watch_release(struct inode *inode, struct file *file) {
	return 0;
}

// XXX fixme, make this thread safe
static ssize_t hw_watch_read(struct file *file, char __user *buf, size_t count,
                             loff_t *f_pos) {
        size_t len = 0;
	//DEFINE_WAIT(wait);

        if(count < sizeof(u32)) {
                return -EINVAL;
        }
        // u32 align count
        count &= ~(sizeof(u32)-1);

        if(wp_buffer_tail == wp_buffer_head) {
                return 0;
        }

        /*
	while(wp_buffer_tail == wp_buffer_head) {
		prepare_to_wait(&hw_watch_queue, &wait, TASK_INTERRUPTIBLE);
		if(wp_buffer_tail == wp_buffer_head) {
			schedule();
		}
		finish_wait(&hw_watch_queue, &wait);
		if(signal_pending(current)) {
			return -ERESTARTSYS;
		}
	}      
        */

        len = 0;
        while((len < count) && (wp_buffer_tail != wp_buffer_head)) {
                if(copy_to_user(buf+len, wp_buffer+wp_buffer_tail, sizeof(u32))) {
                        return -EFAULT;
                }

                len += sizeof(u32);
                if(wp_buffer_tail >= (MAX_STORED_WP-1)) {
                        wp_buffer_tail = 0;
                } else {
                        wp_buffer_tail++;
                }
        }
                
	return len;
}



#define NUM_LINES 8

/*static void test_func(void) {
        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");

        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");
        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");
        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");
        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");
        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");
        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");
        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");
        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");

        asm volatile ("nop; nop; nop; nop; nop; nop; nop; nop;");
	}*/

static int __init hw_watch_init(void)
{
        int idx;
        unsigned long irq_state;

	hw_watch = create_proc_entry("hw_watch", 0, NULL);
	if (!hw_watch)
		return -ENOMEM;

	hw_watch->read_proc = hw_watch_read_proc;
	hw_watch->write_proc = hw_watch_write_proc;
	hw_watch->owner = THIS_MODULE;

        for(idx = 0; idx < MAX_NUM_WP; idx++) {
                wpList[idx].addr = 0;
                wpList[idx].mask = 0;
        }

        memset(&hw_watch_fops, 0, sizeof(hw_watch_fops));
        hw_watch_fops.read = hw_watch_read;
        hw_watch_fops.open = hw_watch_open;
        hw_watch_fops.release = hw_watch_release;
        hw_watch_major = register_chrdev(0, "hw_watch", &hw_watch_fops);
        if(hw_watch_major < 0) {
                printk("could not register hw_watch char device\n");
        }

	printk(KERN_INFO
	       "hw_watch: version %s, Sam King\n",
	       HW_WATCH_VERSION);

	/* Don't go into debug mode on hardware watchpoints */
	disable_dsu_watchpoints(); 

	return 0;
}

static void __exit hw_watch_exit(void)
{
	remove_proc_entry("hw_watch", NULL);
}

module_init(hw_watch_init);
module_exit(hw_watch_exit);

MODULE_AUTHOR("Sam King");
MODULE_DESCRIPTION("Provides control of the front HW_WATCH on LEON systems.");
MODULE_LICENSE("GPL");
MODULE_VERSION(HW_WATCH_VERSION);
