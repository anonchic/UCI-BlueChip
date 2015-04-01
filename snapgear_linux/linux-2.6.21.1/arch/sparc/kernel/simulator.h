#ifndef __SIMULATOR_H__
#define __SIMULATOR_H__

#define NUM_WINDOWS 8
#define NUM_REGS (8 + NUM_WINDOWS*16)

#ifdef __KERNEL__

extern int PRINT_INST;
#define myprintf(format, ...) do{ if(0) printk(KERN_CRIT format, __VA_ARGS__); } while(0)

#include <asm/types.h>
#include <asm/bug.h>
#include <linux/kernel.h>

//typedef __u32 u32;
typedef __s32 i32;
typedef __s64 i64;

#define assert(x) do { if(!(x)) panic("assert failure"); } while(0)

#else

#define PRINT_INST 0
#define myprintf(format, ...) do{ if(0) printf(format, __VA_ARGS__); } while(0)

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>

typedef __uint32_t u32;
typedef __uint64_t u64;
typedef __int32_t i32;
typedef __int64_t i64;
typedef u32 bool;

#endif

#define PRINT_TRAP 0
#define trapprintf(format, ...) do{ if(PRINT_TRAP) printf(format, __VA_ARGS__); } while(0)

#define HANDLE_TRAPS_IN_SIM 0

#define false 0
#define true 1

#define G0 0
#define G1 1
#define G2 2
#define G3 3
#define G4 4
#define G5 5
#define G6 6
#define G7 7

#define O0 8
#define O1 9
#define O2 10
#define O3 11
#define O4 12
#define O5 13
#define O6 14
#define O7 15

#define L0 16
#define L1 17
#define L2 18
#define L3 19
#define L4 20
#define L5 21
#define L6 22
#define L7 23

#define I0 24
#define I1 25
#define I2 26
#define I3 27
#define I4 28
#define I5 29
#define I6 30
#define I7 31

#define SP O6 // That's the letter O

#define ILLEGAL_INSTRUCTION 0x2
#define PRIVILEGED_INSTRUCTION 0x3
#define WINDOW_OVERFLOW 0x5
#define WINDOW_UNDERFLOW 0x6
#define DATA_ACCESS_EXCEPTION 0x9
#define INST_ACCESS_EXCEPTION 0x21

#define PSR_ET_MASK (1 << 5)
#define PSR_PS_MASK (1 << 6)
#define PSR_S_MASK (1 << 7)



struct CPU {
    u32 regs[NUM_REGS];
    u32 pc;
    u32 npc;
    u32 psr;
    u32 wim;
    u32 y;
    u32 tbr;
    u32 branch_pc;
    u32 pending_trap;
    u32 pending_annul;
};

struct control_signals {
    u32 op;
    u32 a;
    u32 rd;
    u32 cond;
    u32 op2;
    u32 op3;
    u32 rs1;
    u32 i;
    u32 asi;
    i32 simm;
    u32 imm;
    u32 imm7;
    u32 disp22;
    u32 disp30;
    u32 rs2;
    u32 raw;
};

#define CWP(cpu) (cpu->psr & 0x1f)
#define SUPERVISOR(cpu) (cpu->psr & (1 << 7))

// these functions need to be implemented to be able to link to 
// the simulator code
void unknownInst(struct CPU *cpu, struct control_signals *control);
bool guestLoad(u32 guestAddr, u32 *value);
bool guestStore(u32 guestAddr, u32 value);


// these are the function that the simulator implements and exports
bool cpu_exec_inst(struct CPU *cpu);
u32 get_reg(struct CPU *cpu, u32 reg);
void set_reg(struct CPU* cpu, u32 reg, u32 value);
void set_cwp(struct CPU* cpu, u32 value);
void increment_cwp(struct CPU* cpu);
void decrement_cwp(struct CPU* cpu);

#endif
