/*
 *  Find trap base address
 *  Get a page sized chunk of memory
 *  Initialize the new memory with the indirection jump table
 *  Change the trap base register to point to the new memory
 *  See what happens
*/

#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/proc_fs.h>
#include <linux/string.h>
#include <linux/ioport.h>
#include <linux/io.h>
#include <asm/traps.h>
#include <asm/uaccess.h>
#include "hyperDefines.h"

#define INTMODULE_VERSION	"0.3"

typedef unsigned int U32;

typedef struct {
	U32 PSR;
	U32 WIM;
	U32 TBR;
} VIRT_PRIV_STATE;

typedef struct {
	U32 g0;
	U32 g1;
	U32 g2;
	U32 g3;
	U32 g4;
	U32 g5;
	U32 g6;
	U32 g7;
	U32 o0;
	U32 o1;
	U32 o2;
	U32 o3;
	U32 o4;
	U32 o5;
	U32 o6;
	U32 o7;
	U32 l0;
	U32 l1;
	U32 l2;
	U32 l3;
	U32 l4;
	U32 l5;
	U32 l6;
	U32 l7;
	U32 i0;
	U32 i1;
	U32 i2;
	U32 i3;
	U32 i4;
	U32 i5;
	U32 i6;
	U32 i7;
	U32 psr;	
} STATE_BACKUP;

typedef struct {
	VIRT_PRIV_STATE state;
	STATE_BACKUP backup;
} VIRTUAL_CPU;

STATE_BACKUP currentWindowLocals = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
VIRTUAL_CPU virtualCPU = {{0,0,0},{0,0,0,0,0,0,0,0}};

extern U32 HYPER_INT_TABLE;
extern U32 trapbase;

static __always_inline void setTBRAddress(U32 address);
static __always_inline U32 getTBRAddress(void);
static __always_inline U32 getTBRCause(void);

/* Set the value of the trap base register to the passed value */
static __always_inline void setTBRAddress(U32 address)
{
	/* Ensure non-address bits are zero */
	address = address & 0xfffff000;

	asm volatile(
		"WR %%g0, %0, %%tbr\n"
		"OR %%g0, %%g0, %%g0\n"
		"OR %%g0, %%g0, %%g0\n"
		"OR %%g0, %%g0, %%g0\n"
		"OR %%g0, %%g0, %%g0\n"
		"OR %%g0, %%g0, %%g0\n"
		:
		: "r" (address)
	);
}

/* Return the value of the trap base register */
static __always_inline U32 getTBRAddress()
{
	register U32 retval;

	asm volatile(
		"RD %%tbr, %0"
		: "=r" (retval)
	);

	/* Clear non-address bits from the tbr value */
	return retval & 0xfffff000;
}

/* Return the cause of the trap */
static __always_inline U32 getTBRCause(void)
{
	register U32 retval;

	asm volatile(
		"RD %%tbr, %0"
		: "=r" (retval)
	);

	/* Clear non-address bits from the tbr value */
	return (retval & 0x00000ff0) >> 4;
}

/* Get to this function when the hypervisor detects a priv. inst. exception */
/* and the virtual state indicates the CPU is in supervisor mode */
/* We need to execute the instruction for the kernel and return to */
/* the next instruction */
/* We are in the next register window (from the one the kernel intended */
/* to execute in.  L1 and L2 contain the trapped instruction address and the */
/* address of the next instruction. */
/* All the locals of this window are backed up in virtualCPU */
void HYPER_DECODE_AND_EXECUTE_INST(void)
{
	U32 * trappedInstAddr;
	U32 * instReturnToAddr;
	U32 psr;
	U32 iccBits;
	U32 currentWindow;
	U32 nextWindow;
	U32 previousWindow;

	asm volatile(
		"mov %%l1, %0\n"
		"mov %%l2, %1\n"
		"rd %%psr, %2\n"
		: "=r" (trappedInstAddr), "=r" (instReturnToAddr), "=r" (psr)
	);

	/* Save the icc from the psr for restoration when we go back to the kernel */
	iccBits = psr & PSR_ICC_MASK;

	/* Determine the previous and next register windows, which must be between 0 and the number of windows - 1 */
	currentWindow = psr & PSR_CWP_MASK;
	nextWindow = (currentWindow > 0) ? currentWindow - 1 : PSR_CWP_ID_MAX;
	previousWindow = (currentWindow < 7) ? currentWindow + 1 : 0;
	
	/* Reserve a place for the instruction to execute and for the branch back to the kernel */
HYPER_INST_PLACEMAT:
	asm volatile(
		"ta 0\n"
		"ta 0\n"
		"nop\n"
	);
}

static int intModule_read_proc(char *buf, char **start, off_t offset2, int count, int *eof, void *data)
{
	U32 * intTable = &HYPER_INT_TABLE;
	U32 * oldIntTable;

	printk(KERN_CRIT "You called the intModule read proc function\n");

	/* Capture the trap base register from Linux */
	printk(KERN_CRIT "Interrupt hijack tester\n");

	if(sizeof(U32) != 4)
	{
		printk(KERN_CRIT "ERROR: unsigned int is not 32-bits wide\n");
		return count;
	}

	/* Redirect the interrupt table to our special version */
	oldIntTable = (U32 *)getTBRAddress();
	printk(KERN_CRIT "Getting the old trap table base address: 0x%.8x\n", (U32)oldIntTable);
	printk(KERN_CRIT "Getting the value of kernel variable trapbase: 0x%.8x\n", (U32)&trapbase);
	printk(KERN_CRIT "Getting the new trap table base address: 0x%.8x\n", (U32)intTable);
	printk(KERN_CRIT "Updating the TBR to the new trap table base address: 0x%.8x\n", (U32)intTable);
	
	/* Set the tbr to my handler */
	setTBRAddress((U32)intTable);
	printk(KERN_CRIT "Getting the trap table base address: 0x%.8x\n", getTBRAddress());

	return 0;	
}

static int intModule_write_proc(struct file *file, const char __user *buffer, unsigned long count, void *data)
{
	printk(KERN_CRIT "You called the intModule write proc function\n");
	return count;
}

static int __init intModueInit(void)
{
	struct proc_dir_entry *intMod;

	intMod = create_proc_entry("intModule", 0, NULL);
	if(!intMod)
	{
		return -ENOMEM;
	}

	intMod->read_proc = intModule_read_proc;
	intMod->write_proc = intModule_write_proc;
	intMod->owner = THIS_MODULE;

	printk(KERN_INFO "intModule: version %s, Matthew Hicks\n", INTMODULE_VERSION);

	return 0;
}

static void __exit intModueExit(void)
{
	remove_proc_entry("intModule", NULL);
}

module_init(intModueInit);
module_exit(intModueExit);

MODULE_AUTHOR("Matthew Hicks");
MODULE_DESCRIPTION("Takes over the interrupt table of Linux, checking to make sure our handling of register windows works");
MODULE_LICENSE("GPL");
MODULE_VERSION(INTMODULE_VERSION);
