/*
 * University of Illinois/NCSA
 * Open Source License
 *
 *  Copyright (c) 2007-2008,The Board of Trustees of the University of
 *  Illinois.  All rights reserved.
 *
 *  Copyright (c) 2009 Sam King
 *
 *  Developed by:
 *
 *  Professor Sam King in the Department of Computer Science
 *  The University of Illinois at Urbana-Champaign
 *      http://www.cs.uiuc.edu/homes/kingst/Research.html
 *
 *       Permission is hereby granted, free of charge, to any person
 *       obtaining a copy of this software and associated
 *       documentation files (the "Software"), to deal with the
 *       Software without restriction, including without limitation
 *       the rights to use, copy, modify, merge, publish, distribute,
 *       sublicense, and/or sell copies of the Software, and to permit
 *       persons to whom the Software is furnished to do so, subject
 *       to the following conditions:
 *
 *          Redistributions of source code must retain the above
 *          copyright notice, this list of conditions and the
 *          following disclaimers.
 *
 *          Redistributions in binary form must reproduce the above
 *          copyright notice, this list of conditions and the
 *          following disclaimers in the documentation and/or other
 *          materials provided with the distribution.
 *
 *          Neither the names of Sam King, the University of Illinois,
 *          nor the names of its contributors may be used to endorse
 *          or promote products derived from this Software without
 *          specific prior written permission.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT.  IN NO EVENT SHALL THE CONTRIBUTORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 *  DEALINGS WITH THE SOFTWARE.
*/

#include "simulator.h"
//#include "priv.h"

#define OP_FORMAT_1     0x1
#define OP_FORMAT_2     0x0
#define OP_FORMAT_3_ALU 0x2
#define OP_FORMAT_3_MEM 0x3

#define OP2_NOP         0x0
#define OP2_BRANCH      0x2
#define OP2_SETHI       0x4

#define OP3_ADD         0x0
#define OP3_AND         0x1 
#define OP3_OR          0x2
#define OP3_XOR         0x3
#define OP3_SUB         0x4
#define OP3_ANDN        0x5
#define OP3_ORN         0x6
#define OP3_UMUL        0xa
#define OP3_SMUL        0xb
#define OP3_UDIV        0xe
#define OP3_SDIV        0xf
#define OP3_ADDCC       0x10
#define OP3_ANDCC       0x11
#define OP3_ORCC        0x12
#define OP3_XORCC       0x13
#define OP3_SUBCC       0x14
#define OP3_UMULCC      0x1a
#define OP3_SMULCC      0x1b
#define OP3_UDIVCC      0x1e
#define OP3_SDIVCC      0x1f
#define OP3_MULSCC      0x24
#define OP3_SLL         0x25
#define OP3_SRL         0x26
#define OP3_SRA         0x27
#define OP3_RDASR       0x28 /* also RDY */
#define OP3_RDPSR       0x29
#define OP3_RDWIM       0x2a
#define OP3_RDTBR       0x2b
#define OP3_WRASR       0x30 /* also WRY */
#define OP3_WRPSR       0x31
#define OP3_WRWIM       0x32
#define OP3_WRTBR       0x33
#define OP3_JMPL        0x38
#define OP3_RETT        0x39
#define OP3_TICC        0x3a

#define OP3_LD          0x0
#define OP3_LDUB        0x1
#define OP3_LDUH        0x2
#define OP3_LDD         0x3
#define OP3_ST          0x4
#define OP3_STB         0x5
#define OP3_STH         0x6
#define OP3_STD         0x7
#define OP3_LDSB        0x9
#define OP3_LDSH        0xa
#define OP3_LDSTUB      0xd
#define OP3_SAVE        0x3c
#define OP3_RESTORE     0x3d

#define COND_N          0x0
#define COND_E          0x1
#define COND_LE         0x2
#define COND_L          0x3
#define COND_ALWAYS     0x8
#define COND_NE         0x9
#define COND_G          0xa
#define COND_CC         0xd



/* trap types moved to .h */

#define IS_BIT_SET(a,bit) (((a) & (0x1 << (bit))) != 0)
#define IS_ICC_N(a) (IS_BIT_SET((a), 23))
#define IS_ICC_Z(a) (IS_BIT_SET((a), 22))
#define IS_ICC_V(a) (IS_BIT_SET((a), 21))
#define IS_ICC_C(a) (IS_BIT_SET((a), 20))

static void set_pending_trap(struct CPU *cpu, u32 tt) {
    assert((tt & 0xffffff00) == 0);
    cpu->pending_trap = tt;
}

u32 get_reg(struct CPU *cpu, u32 reg) {
    if (reg < 8) {
	return cpu->regs[reg];
    } else {
	u32 new_reg = (reg + 16 * CWP(cpu));
	if (new_reg >= NUM_REGS)
	    new_reg = (new_reg % NUM_REGS) + 8;
	return cpu->regs[new_reg];
    }
}

void set_reg(struct CPU* cpu, u32 reg, u32 value) {
    if (reg < 8) {
	cpu->regs[reg] = value;
    } else {
	u32 new_reg = (reg + 16 * CWP(cpu));
	if (new_reg >= NUM_REGS)
	    new_reg = (new_reg % NUM_REGS) + 8;
	cpu->regs[new_reg] = value;
    }
}

void set_cwp(struct CPU* cpu, u32 value) {
    assert((value & ~0x1f) == 0);
    cpu->psr &= ~0x1f;
    cpu->psr |= value; 
}

void increment_cwp(struct CPU* cpu) {
    u32 new_CWP = (CWP(cpu) + NUM_WINDOWS + 1) % NUM_WINDOWS;
    set_cwp(cpu, new_CWP);
}

void decrement_cwp(struct CPU* cpu) {
    u32 new_CWP = (CWP(cpu) + NUM_WINDOWS - 1) % NUM_WINDOWS;
    set_cwp(cpu, new_CWP);
}

static u32 fetch(struct CPU *cpu) {
    u32 inst = 0xdeadbeef;
    assert((cpu->pc & 0x3) == 0);
    if(!guestLoad(cpu->pc, &inst)) {
        set_pending_trap(cpu, INST_ACCESS_EXCEPTION);
    }
    return inst;
}

static i32 sign_extend(u32 value, u32 sign_bit) {
    if(IS_BIT_SET(value, sign_bit)) {
        value = value | (0xffffffff << sign_bit);
    }

    return (i32) value;
}


static i32 sign_extend_22(u32 disp22) {
    return sign_extend(disp22, 21);
}

static i32 sign_extend_13(u32 imm) {
    return sign_extend(imm, 12);
}

static void decode_control_signals(u32 inst, struct control_signals *control) {
    control->raw = inst;
    control->op = (inst >> 30) & 0x3;
    control->a = (inst >> 29) & 0x1;
    control->rd = (inst >> 25) & 0x1f;
    control->op2 = (inst >> 22) & 0x7;
    control->op3 = (inst >> 19) & 0x3f;
    control->rs1 = (inst >> 14) & 0x1f;
    control->i = (inst >> 13) & 0x1;
    control->asi = (inst >> 5) & 0xff;
    control->simm = sign_extend_13(inst & 0x1fff);
    control->imm = inst & 0x3fffff;
    control->imm7 = inst & 0x7f;
    control->rs2 = inst & 0x1f;
    control->disp22 = inst & 0x3fffff;
    control->disp30 = inst & 0x3fffffff;
    control->cond = (inst >> 25) & 0xf;
}

static bool pending_icc_n = 0;
static bool pending_icc_z = 0;
static bool pending_icc_v = 0;
static bool pending_icc_c = 0;
  
static void gen_icc(u32 value, bool overflow, bool carry) {
    // icc_n
    if(IS_BIT_SET(value, 31)) {
	pending_icc_n = 1;
    } else {
        pending_icc_n = 0;
    }

    // icc_z
    if(value == 0) {
        pending_icc_z = 1;
    } else {
        pending_icc_z = 0;
    }

    if (overflow) {
	pending_icc_v = 1;
    } else {
	pending_icc_v = 0;
    }

    if (carry) {
	pending_icc_c = 1;
    } else {
	pending_icc_c = 0;
    }
}

static void apply_pending_icc(struct CPU *cpu) {
    myprintf("setting CC\%s","\n");

    cpu->psr &= ~(0x1 << 23);
    cpu->psr &= ~(0x1 << 22);
    cpu->psr &= ~(0x1 << 21);
    cpu->psr &= ~(0x1 << 20);

    cpu->psr |= (pending_icc_n << 23);
    cpu->psr |= (pending_icc_z << 22);
    cpu->psr |= (pending_icc_v << 21);
    cpu->psr |= (pending_icc_c << 20);
}


/********************** ALU instructions *****************/
static void add(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("add r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    set_reg(cpu, rd, result);
    /* FIXME: Compute carry and overflow */
    gen_icc(result, 0, 0);
}

static void addi(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    myprintf("addi r%u, %d, r%u\n", rs1, signedImm, rd);
    u32 result = get_reg(cpu, rs1) + signedImm;
    set_reg(cpu, rd, result);
    /* FIXME: Compute carry and overflow */
    gen_icc(result, 0, 0);
}

static void and(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("and r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = get_reg(cpu, rs1) & get_reg(cpu, rs2);
    set_reg(cpu, rd, result);
    gen_icc(result, 0,0);
}

static void andi(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    myprintf("andi r%u, %d, r%u\n", rs1, signedImm, rd);
    u32 result = get_reg(cpu, rs1) & signedImm;
    set_reg(cpu, rd, result);
    gen_icc(result, 0,0);
}

static void andn(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("and r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = get_reg(cpu, rs1) & ~get_reg(cpu, rs2);
    set_reg(cpu, rd, result);
}

static void andni(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    myprintf("andi r%u, %d, r%u\n", rs1, signedImm, rd);
    u32 result = get_reg(cpu, rs1) & ~signedImm;
    set_reg(cpu, rd, result);
}

static void or(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("or r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = get_reg(cpu, rs1) | get_reg(cpu, rs2);
    set_reg(cpu, rd, result);
    gen_icc(result, 0, 0);
}

static void ori(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    myprintf("ori r%u, %d, r%u\n", rs1, signedImm, rd);
    u32 result = get_reg(cpu, rs1) | signedImm;
    set_reg(cpu, rd, result);
}

static void orn(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("orn r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = get_reg(cpu, rs1) | ~get_reg(cpu, rs2);
    set_reg(cpu, rd, result);
}

static void orni(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    myprintf("orni r%u, %d, r%u\n", rs1, signedImm, rd);
    u32 result = get_reg(cpu, rs1) | ~signedImm;
    set_reg(cpu, rd, result);
}

static void sub(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("sub r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = get_reg(cpu, rs1) - get_reg(cpu, rs2);

    u64 long_result = (u64)get_reg(cpu, rs1) - (u64)get_reg(cpu, rs2);
    bool overflow, carry;
    
    if (long_result != result) {
	overflow = true;
    } else {
	overflow = false;
    }

    u64 long_rs1_with_a_one;
    long_rs1_with_a_one = (u64)get_reg(cpu, rs1) | ((u64)1 << 32);    
    long_result = long_rs1_with_a_one - (u64)get_reg(cpu, rs2);
    if (long_result & ((u64)1 << 32)) {    
	carry = false;
    } else {
	carry = true;
    }

    gen_icc(result, overflow, carry);
    set_reg(cpu, rd, result);
}

static void subi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("subi r%u, %d, r%u\n", rs1, simm, rd);

    u32 result = get_reg(cpu, rs1) - simm;

    u64 long_result = (u64)get_reg(cpu, rs1) - (u64)simm;
    bool overflow, carry;
    
    if (long_result != result) {
	overflow = true;
    } else {
	overflow = false;
    }

    u64 long_rs1_with_a_one;
    long_rs1_with_a_one = (u64)get_reg(cpu, rs1) | ((u64)1 << 32);    
    long_result = long_rs1_with_a_one - (u64)simm;
    if (long_result & ((u64)1 << 32)) {    
	carry = false;
    } else {
	carry = true;
    }
    
    gen_icc(result, overflow, carry);
    set_reg(cpu, rd, result);
}

static void xor(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("xor r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = get_reg(cpu, rs1) ^ get_reg(cpu, rs2);
    set_reg(cpu, rd, result);
    gen_icc(result, 0, 0);
}

static void xori(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    myprintf("xori r%u, %d, r%u\n", rs1, signedImm, rd);
    u32 result = get_reg(cpu, rs1) ^ signedImm;
    set_reg(cpu, rd, result);
    gen_icc(result, 0, 0);
}

static void sll(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("sll r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = (unsigned)get_reg(cpu,rs1) << (unsigned)(get_reg(cpu, rs2) & 0x1F);
    set_reg(cpu, rd, result);
}

static void slli(struct CPU *cpu, u32 rd, u32 rs1, u32 simm) {
    myprintf("slli r%u, %d, r%u\n", rs1, simm, rd);
    u32 result = (unsigned)get_reg(cpu,rs1) << (unsigned)(simm & 0x1F);
    set_reg(cpu, rd, result);
}

static void srl(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("srl r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = (unsigned)get_reg(cpu,rs1) >> (unsigned)(get_reg(cpu, rs2) & 0x1F);
    set_reg(cpu, rd, result);
}

static void srli(struct CPU *cpu, u32 rd, u32 rs1, u32 simm) {
    myprintf("srli r%u, %d, r%u\n", rs1, simm, rd);
    u32 result = (unsigned)get_reg(cpu,rs1) >> (unsigned)(simm & 0x1F);
    set_reg(cpu, rd, result);
}

static void sra(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("sra r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 result = (signed)get_reg(cpu,rs1) >> (signed)(get_reg(cpu, rs2) & 0x1F);
    set_reg(cpu, rd, result);
}

static void srai(struct CPU *cpu, u32 rd, u32 rs1, u32 simm) {
    myprintf("srai r%u, %d, r%u\n", rs1, simm, rd);
    u32 result = (signed)get_reg(cpu,rs1) >> (signed)(simm & 0x1F);
    set_reg(cpu, rd, result);
}

static void jmpl(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("jmpl r%u, r%u, r%u\n", rs1, rs2, rd);
    cpu->branch_pc = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    set_reg(cpu, rd, cpu->pc);
}

static void jmpli(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("jmpli r%u, %d, r%u\n", rs1, simm, rd);
    cpu->branch_pc = get_reg(cpu, rs1) + simm;
    set_reg(cpu, rd, cpu->pc);
}

static void rett(struct CPU *cpu, u32 rs1, u32 rs2) {
    myprintf("rett r%u, r%u, \n", rs1, rs2);

    // TODO: more traps can happen

    u32 new_CWP = (CWP(cpu) + 1) % NUM_WINDOWS;

    if (cpu->wim & (1 << new_CWP)) {
	set_pending_trap(cpu, WINDOW_UNDERFLOW);
    } else if (cpu->psr & PSR_ET_MASK && !(cpu->psr & PSR_S_MASK)) {
	set_pending_trap(cpu, PRIVILEGED_INSTRUCTION);
    } else if (cpu->psr & PSR_ET_MASK && cpu->psr & PSR_S_MASK) {
	set_pending_trap(cpu, ILLEGAL_INSTRUCTION);
    } else {
	cpu->branch_pc = get_reg(cpu, rs1) + get_reg(cpu, rs2);
	cpu->psr &= ~PSR_S_MASK;
	if (cpu->psr & PSR_PS_MASK) {
	    cpu->psr |= PSR_S_MASK;
	}
	cpu->psr |= PSR_ET_MASK;
	set_cwp(cpu, new_CWP);
    }
}

static void retti(struct CPU *cpu, u32 rs1, u32 simm) {
    myprintf("retti r%u, %d, \n", rs1, simm);

    // TODO: more traps can happen

    u32 new_CWP = (CWP(cpu) + 1) % NUM_WINDOWS;

    if (cpu->wim & (1 << new_CWP)) {
	set_pending_trap(cpu, WINDOW_UNDERFLOW);
    } else if (cpu->psr & PSR_ET_MASK && !(cpu->psr & PSR_S_MASK)) {
	set_pending_trap(cpu, PRIVILEGED_INSTRUCTION);
    } else if (cpu->psr & PSR_ET_MASK && cpu->psr & PSR_S_MASK) {
	set_pending_trap(cpu, ILLEGAL_INSTRUCTION);
    } else {
	cpu->branch_pc = get_reg(cpu, rs1) + simm;
	cpu->psr &= ~PSR_S_MASK;
	if (cpu->psr & PSR_PS_MASK) {
	    cpu->psr |= PSR_S_MASK;
	}
	cpu->psr |= PSR_ET_MASK;
	set_cwp(cpu, new_CWP);
    }
}

static void sethi(struct CPU *cpu, u32 rd, u32 imm) {
    myprintf("sethi %u, r%u\n", imm, rd);
    set_reg(cpu, rd, imm << 10);
}

/*********************************************************/

/********************** multiply / divide inst *************************/

static void mulscc(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("mulscc r%u, r%u, r%u\n", rs1, rs2, rd);
    u32 multiplier = get_reg(cpu, rs2);
    i32 orig_rs1 = get_reg(cpu, rs1);

    u32 n = (cpu->psr & (1 << 23)) >> 23;
    u32 v = (cpu->psr & (1 << 21)) >> 21;
    u32 new_msb = n ^ v;
    
    u32 value = get_reg(cpu, rs1) >> 1 | new_msb << 31;

    if (cpu->y & 1) {
	set_reg(cpu, rd, multiplier + value);
	bool overflow;
	if (((u64)multiplier + (u64)value) != (u32)(multiplier + value)) {
	    overflow = true;
	} else {
	    overflow = false;
	}
	gen_icc(multiplier + value, overflow, 0);
    } else {
	set_reg(cpu, rd, value);
	gen_icc(value, 0, 0);
    }
    
    apply_pending_icc(cpu);

    cpu->y = (cpu->y >> 1) | (orig_rs1 << 31);
}

static void mulscci(struct CPU *cpu, u32 rd, u32 rs1, u32 signedImm) {
    myprintf("mulscci r%u, %d, r%u\n", rs1, signedImm, rd);
    u32 multiplier = signedImm;
    i32 orig_rs1 = get_reg(cpu, rs1);

    u32 n = (cpu->psr & (1 << 23)) >> 23;
    u32 v = (cpu->psr & (1 << 21)) >> 21;
    u32 new_msb = n ^ v;
    
    u32 value = get_reg(cpu, rs1) >> 1 | new_msb << 31;

    if (cpu->y & 1) {
	set_reg(cpu, rd, multiplier + value);
	bool overflow;
	if (((u64)multiplier + (u64)value) != (u32)(multiplier + value)) {
	    overflow = true;
	} else {
	    overflow = false;
	}
	gen_icc(multiplier + value, overflow, 0);
    } else {
	set_reg(cpu, rd, value);
	gen_icc(value, 0, 0);
    }
    
    apply_pending_icc(cpu);

    cpu->y = (cpu->y >> 1) | (orig_rs1 << 31);
}

static void umul(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("umul r%u, r%u, r%u\n", rs1, rs2, rd);
    u64 result = (unsigned)get_reg(cpu, rs1) * (unsigned)get_reg(cpu, rs2);
    set_reg(cpu, rd, result & 0xFFFFFFFF);
    cpu->y = (result >> 32); 
    gen_icc(result & 0xFFFFFFFF, 0, 0);
}

static void umulcc(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    umul(cpu, rd, rs1, rs2);
    apply_pending_icc(cpu);
}

static void umuli(struct CPU *cpu, u32 rd, u32 rs1, u32 signedImm) {
    myprintf("umuli r%u, %u, r%u\n", rs1, signedImm, rd);
    u64 result = (unsigned)get_reg(cpu, rs1) * (unsigned)signedImm;
    set_reg(cpu, rd, result & 0xFFFFFFFF);
    cpu->y = (result >> 32);
    gen_icc(result & 0xFFFFFFFF, 0, 0);
}

static void umulcci(struct CPU *cpu, u32 rd, u32 rs1, u32 signedImm) {
    umuli(cpu, rd, rs1, signedImm);
    apply_pending_icc(cpu);
}

static void smul(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("smul r%u, r%u, r%u\n", rs1, rs2, rd);
    u64 result = (signed)get_reg(cpu, rs1) * (signed)get_reg(cpu, rs2);
    set_reg(cpu, rd, result & 0xFFFFFFFF);
    cpu->y = (result >> 32);
    gen_icc(result & 0xFFFFFFFF, 0, 0);
}

static void smulcc(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    smul(cpu, rd, rs1, rs2);
    apply_pending_icc(cpu);
}

static void smuli(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    myprintf("smuli r%u, %u, r%u\n", rs1, signedImm, rd);
    u64 result = (signed)get_reg(cpu, rs1) * (signed)signedImm;
    set_reg(cpu, rd, result & 0xFFFFFFFF);
    cpu->y = (result >> 32);
    gen_icc(result & 0xFFFFFFFF, 0, 0);
}

static void smulcci(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    smuli(cpu, rd, rs1, signedImm);
    apply_pending_icc(cpu);
}
    

static void udiv(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("udiv r%u, r%u, r%u\n", rs1, rs2, rd);
    u64 numerator = ((u64)cpu->y << 32) | get_reg(cpu, rs1);
    u64 result64 = numerator / (unsigned)get_reg(cpu, rs2);
    u32 result32;
    bool overflow = false;

    if (result64 > 0xFFFFFFFF) {
	overflow = true;
	result32 = 0xFFFFFFFF;
    } else {
	result32 = result64 & 0xFFFFFFFF;
    }
    
    set_reg(cpu, rd, result32);
    gen_icc(result32, overflow, 0);
    
}

static void udivcc(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    udiv(cpu, rd, rs1, rs2);
    apply_pending_icc(cpu);
}

static void udivi(struct CPU *cpu, u32 rd, u32 rs1, u32 signedImm) {
    myprintf("udivi r%u, %d, r%u\n", rs1, signedImm, rd);
    u64 numerator = ((u64)cpu->y << 32) | get_reg(cpu, rs1);
    u64 result64 = numerator / (unsigned)signedImm;
    u32 result32;
    bool overflow = false;

    if (result64 > 0xFFFFFFFF) {
	overflow = true;
	result32 = 0xFFFFFFFF;
    } else {
	result32 = result64 & 0xFFFFFFFF;
    }

    set_reg(cpu, rd, result32);
    gen_icc(result32, overflow, 0);
}

static void udivcci(struct CPU *cpu, u32 rd, u32 rs1, u32 signedImm) {
    udivi(cpu, rd, rs1, signedImm);
    apply_pending_icc(cpu);
}

static void sdiv(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("sdiv r%u, r%u r%u\n", rs1, rs2, rd);
    u64 numerator = ((i64)cpu->y << 32) | get_reg(cpu, rs1);
    i64 result64 = numerator / (signed)get_reg(cpu, rs2);
    bool overflow = false;
    i32 result32;
    if (result64 > 0x7FFFFFFF) {
	result32 = 0x7FFFFFFF;
	overflow = true;
    } else if (result64 < -((i64)1<<31)) {
	result32 = -((i64)1<<31);
	overflow = true;
    } else {
	result32 = result64 & 0xFFFFFFFF;
    }
    set_reg(cpu, rd, result32);
    gen_icc(result32, overflow, 0);
}

static void sdivcc(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    sdiv(cpu, rd, rs1, rs2);
    apply_pending_icc(cpu);
}

static void sdivi(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    myprintf("sdivi r%u, %d, r%u\n", rs1, signedImm, rd);
    u64 numerator = ((i64)cpu->y << 32) | get_reg(cpu, rs1);
    i64 result64 = numerator / (signed)signedImm;
    i32 result32;
    bool overflow = false;
    if (result64 > 0x7FFFFFFF) {
	result32 = 0x7FFFFFFF;
	overflow = true;
    } else if (result64 < -((i64)1<<31)) {
	result32 = -((i64)1 << 31);
	overflow = true;
    } else {
	result32 = result64 & 0xFFFFFFFF;
    }
    set_reg(cpu, rd, result32);
    gen_icc(result32, overflow, 0);
}
static void sdivcci(struct CPU *cpu, u32 rd, u32 rs1, i32 signedImm) {
    sdivi(cpu, rd, rs1, signedImm);
    apply_pending_icc(cpu);
}


/*********************************************************/

/********************** privilaged inst *************************/
static void rdpsr(struct CPU *cpu,  u32 rd) {
    myprintf("rdpsr r%u\n", rd);
    set_reg(cpu, rd, cpu->psr);
}

static void rdwim(struct CPU *cpu,  u32 rd) {
    myprintf("rdwim r%u\n", rd);
    set_reg(cpu, rd, cpu->wim);
}

static void rdtbr(struct CPU *cpu,  u32 rd) {
    myprintf("rdtbr r%u\n", rd);
    set_reg(cpu, rd, cpu->tbr);
}

static void rdy(struct CPU *cpu, u32 rd) {
    myprintf("rdy r%u\n", rd);
    set_reg(cpu, rd, cpu->y);
}

static void wry(struct CPU * cpu, u32 rs1, u32 rs2) {
    myprintf("wry r%u XOR r%u\n", rs1, rs2);
    cpu->y = get_reg(cpu, rs1) ^ get_reg(cpu, rs2);    
}

static void wryi(struct CPU * cpu, u32 rs1, i32 signedImm) {
    myprintf("wryi r%u XOR %d\n", rs1, signedImm);
    cpu->y = get_reg(cpu, rs1) ^ signedImm;    
}

static void wrpsr(struct CPU *cpu, u32 rs1, u32 rs2) {
    myprintf("wrpsr r%u XOR r%u\n", rs1, rs2);
    u32 result = get_reg(cpu, rs1) ^ get_reg(cpu, rs2);

    /* if new CWP is unimplemented, trap illegal instruction */
    if ((result & 0x1f) > NUM_WINDOWS) {
	set_pending_trap(cpu, ILLEGAL_INSTRUCTION);
    } else {
	cpu->psr = result;
    }
}

static void wrpsri(struct CPU *cpu, u32 rs1, i32 signedImm) {
    myprintf("wrpsri r%u XOR %d\n", rs1, signedImm);
    u32 result = get_reg(cpu, rs1) ^ signedImm;

    /* if new CWP is unimplemented, trap illegal instruction */
    if ((result & 0x1f) > NUM_WINDOWS) {
	set_pending_trap(cpu, ILLEGAL_INSTRUCTION);
    } else {
	cpu->psr = result;
    }
}

static void wrwim(struct CPU* cpu, u32 rs1, u32 rs2) {
    myprintf("wrwim r%u XOR r%u\n", rs1, rs2);
    cpu->wim = get_reg(cpu, rs1) ^ get_reg(cpu, rs2);
}

static void wrwimi(struct CPU* cpu, u32 rs1, i32 signedImm) {
    myprintf("wrwimi r%u XOR %d\n", rs1, signedImm);
    cpu->wim = get_reg(cpu, rs1) ^ signedImm;
}

static void wrtbr(struct CPU* cpu, u32 rs1, u32 rs2) {
    myprintf("wrtbr r%u XOR r%u\n", rs1, rs2);
    /* wrtbr does not affect lowest twelve bits */
    u32 mask = ~0xFFF;
    cpu->tbr &= ~mask;
    cpu->tbr |= ((get_reg(cpu, rs1) ^ get_reg(cpu, rs2)) & mask);
}

static void wrtbri(struct CPU* cpu, u32 rs1, i32 signedImm) {
    myprintf("wrtbri r%u XOR %d\n", rs1, signedImm);
    cpu->tbr = get_reg(cpu, rs1) ^ signedImm;
}



/*********************************************************/


/********************** mem inst *************************/

static void ldi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("ldi [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;
    u32 dest;
    if(!guestLoad(guestAddr, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
    set_reg(cpu, rd, dest);
}

static void ld(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("ld [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    u32 dest;
    if(!guestLoad(guestAddr, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
    set_reg(cpu, rd, dest);
}

static void lduh(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("lduh [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);

    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
    
    u32 shift;
    if ((guestAddr & 0x3) == 0) {
	shift = 16;
    } else {
	shift = 0;
    }

    set_reg(cpu, rd, (dest >> shift) & 0xFFFF);
}

static void lduhi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("lduhi [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;

    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }

    u32 shift;
    if ((guestAddr & 0x3) == 0) {
	shift = 16;
    } else {
	shift = 0;
    }
    
    set_reg(cpu, rd, (dest >> shift) & 0xFFFF);
}

static void ldub(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("ldub [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 

    u32 shift = (0x3-(guestAddr & 0x3)) * 8;
    set_reg(cpu, rd, (dest >> shift) & 0xFF);    
}

static void ldubi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("ldubi [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;
    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 

    u32 shift = (0x3-(guestAddr & 0x3)) * 8;
    set_reg(cpu, rd, (dest >> shift) & 0xFF);    
}

static void ldsh(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("ldsh [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }

    u32 shift;
    if ((guestAddr & 0x3) == 0) {
	shift = 0;
    } else {
	shift = 16;
    }

    u32 left_aligned = dest << shift;
    set_reg(cpu, rd, ((signed)left_aligned) >> 16);
}

static void ldshi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("ldshi [r%u+%d], %d\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;
    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }

    u32 shift;
    if ((guestAddr & 0x3) == 0) {
	shift = 0;
    } else {
	shift = 16;
    }
    
    u32 left_aligned = dest << shift;
    set_reg(cpu, rd, ((signed)left_aligned) >> 16);
}

static void ldsb(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("ldsb [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 

    u32 left_shift = (guestAddr & 0x03) * 8;
    u32 left_aligned = dest << left_shift;
    set_reg(cpu, rd, ((signed)left_aligned) >> 24);    
}

static void ldsbi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("ldsbi [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;
    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 

    u32 left_shift = (guestAddr & 0x03) * 8;
    u32 left_aligned = dest << left_shift;
    set_reg(cpu, rd, ((signed)left_aligned) >> 24);    
}


static void ldd(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("ldd [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    u32 dest;
    u32 dest2;
    if(!guestLoad(guestAddr, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
    if(!guestLoad(guestAddr+4, &dest2)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
    set_reg(cpu, rd, dest);
    set_reg(cpu, rd+1, dest2);
}

static void lddi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("lddi [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;
    u32 dest;
    u32 dest2;
    if(!guestLoad(guestAddr, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
    if(!guestLoad(guestAddr+4, &dest2)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
    set_reg(cpu, rd, dest);
    set_reg(cpu, rd+1, dest2);
}

static void ldstub(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("ldstub [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 

    u32 shift = (0x3-(guestAddr & 0x3)) * 8;
    set_reg(cpu, rd, dest >> shift);    

    dest |= 0xFF << shift;

    if(!guestStore(guestAddr & ~0x3, dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 
}

static void ldstubi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("ldstubi [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;
    u32 dest;
    if(!guestLoad(guestAddr & ~0x3, &dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 

    u32 shift = (0x3-(guestAddr & 0x3)) * 8;
    set_reg(cpu, rd, dest >> shift);    

    dest |= 0xFF << shift;

    if(!guestStore(guestAddr & ~0x3, dest)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 
}

static void sti(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("sti [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;
    if(!guestStore(guestAddr, get_reg(cpu, rd)) ){
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
}

static void st(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("st [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    if(!guestStore(guestAddr, get_reg(cpu, rd))) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
}

static void stb(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("stb [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);

    u32 dest_word;
    if(!guestLoad(guestAddr & ~0x3, &dest_word)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 
    
    u32 shift = (0x3-(guestAddr & 0x3)) * 8;
    u32 mask = 0xFF << shift;

    dest_word &= ~mask;
    dest_word |= (get_reg(cpu, rd) & 0xFF) << shift;
    
    if(!guestStore(guestAddr & ~0x3, dest_word)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
}

static void stbi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("stbi [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;

    u32 dest_word;
    if(!guestLoad(guestAddr & ~0x3, &dest_word)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 
    
    u32 shift = (0x3-(guestAddr & 0x3)) * 8;
    u32 mask = 0xFF << shift;

    dest_word &= ~mask;
    dest_word |= (get_reg(cpu, rd) & 0xFF) << shift;

    if(!guestStore(guestAddr & ~0x3, dest_word)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
}

static void sth(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("sth [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);

    u32 dest_word;
    if(!guestLoad(guestAddr & ~0x3, &dest_word)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 

    u32 shift;
    if ((guestAddr & 0x3) == 0) {
	shift = 16;
    } else {
	shift = 0;
    }

    u32 mask = 0xFFFF << shift;

    dest_word &= ~mask;
    dest_word |= (get_reg(cpu, rd) & 0xFFFF) << shift;
    
    if(!guestStore(guestAddr & ~0x3, dest_word)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
}

static void sthi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("sthi [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;

    u32 dest_word;
    if(!guestLoad(guestAddr & ~0x3, &dest_word)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    } 

    u32 shift;
    if ((guestAddr & 0x3) == 0) {
	shift = 16;
    } else {
	shift = 0;
    }

    u32 mask = 0xFFFF << shift;

    dest_word &= ~mask;
    dest_word |= (get_reg(cpu, rd) & 0xFFFF) << shift;

    if(!guestStore(guestAddr & ~0x3, dest_word)) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
}

static void stdi(struct CPU *cpu, u32 rd, u32 rs1, i32 simm) {
    myprintf("stdi [r%u+%d], r%u\n", rs1, simm, rd);
    u32 guestAddr = get_reg(cpu, rs1) + simm;
    if(!guestStore(guestAddr, get_reg(cpu, rd)) ){
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
    if(!guestStore(guestAddr+4, get_reg(cpu, rd+1)) ){
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
}

static void std(struct CPU *cpu, u32 rd, u32 rs1, u32 rs2) {
    myprintf("std [r%u+r%u], r%u\n", rs1, rs2, rd);
    u32 guestAddr = get_reg(cpu, rs1) + get_reg(cpu, rs2);
    if(!guestStore(guestAddr, get_reg(cpu, rd))) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
    if(!guestStore(guestAddr+4, get_reg(cpu, rd+1))) {
        set_pending_trap(cpu, DATA_ACCESS_EXCEPTION);
    }
}

/*********************************************************/


/********************** branch inst **********************/
static void bne(struct CPU *cpu, u32 disp22, bool annul) {
    myprintf("bne %u %d\n", disp22, sign_extend_22(disp22));
    if(!IS_ICC_Z(cpu->psr)) {
        cpu->branch_pc = cpu->pc + (4 * sign_extend_22(disp22));
    } else if (annul) {
	cpu->pending_annul = 1;
    }
}

static void be(struct CPU *cpu, u32 disp22, bool annul) {
    myprintf("be %u %d\n", disp22, sign_extend_22(disp22));
    if(IS_ICC_Z(cpu->psr)) {
        cpu->branch_pc = cpu->pc + (4 * sign_extend_22(disp22));
    } else if (annul) {
	cpu->pending_annul = 1;
    }
}

static void bn(struct CPU *cpu, u32 disp22, bool annul) {
    myprintf("bn %u %d\n", disp22, sign_extend_22(disp22));
    if (annul) {
	cpu->pending_annul = 1;
    }
}

static void bcc(struct CPU *cpu, u32 disp22, bool annul) {
    myprintf("bcc %u %d\n", disp22, sign_extend_22(disp22));
    if(!IS_ICC_C(cpu->psr)) {
        cpu->branch_pc = cpu->pc + (4 * sign_extend_22(disp22));
    } else if (annul) {
	cpu->pending_annul = 1;
    }
}

static void b(struct CPU *cpu, u32 disp22, bool annul) {
    myprintf("b %u %d\n", disp22, sign_extend_22(disp22));
    cpu->branch_pc = cpu->pc + (4 * sign_extend_22(disp22));
    if (annul) {
	cpu->pending_annul = 1;
    }
}

static void ble(struct CPU *cpu, u32 disp22, bool annul) {
    myprintf("ble %u %d\n", disp22, sign_extend_22(disp22));
    if(IS_ICC_Z(cpu->psr) || (IS_ICC_N(cpu->psr) ^ IS_ICC_V(cpu->psr))) {
        cpu->branch_pc = cpu->pc + (4 * sign_extend_22(disp22));
    } else if (annul) {
	cpu->pending_annul = 1;
    }
}

static void bg(struct CPU *cpu, u32 disp22, bool annul) {
    myprintf("bg %u %d\n", disp22, sign_extend_22(disp22));
    if(!(IS_ICC_Z(cpu->psr) || (IS_ICC_N(cpu->psr) ^ IS_ICC_V(cpu->psr)))) {
        cpu->branch_pc = cpu->pc + (4 * sign_extend_22(disp22));
    } else if (annul) {
	cpu->pending_annul = 1;
    }
}

static void bl(struct CPU *cpu, u32 disp22, bool annul) {
    myprintf("bl %u %d\n", disp22, sign_extend_22(disp22));
    if(IS_ICC_N(cpu->psr) ^ IS_ICC_V(cpu->psr)) {
        cpu->branch_pc = cpu->pc + (4 * sign_extend_22(disp22));
    } else if (annul) {
	cpu->pending_annul = 1;
    }
}

static void call(struct CPU *cpu, u32 disp30) {
    myprintf("call %u\n", disp30);
    set_reg(cpu, 15, cpu->pc); // 15 is o7
    cpu->branch_pc = cpu->pc + (4 * disp30);    
}

/*********************************************************/

static void processBranch(struct CPU *cpu, struct control_signals *control) {
    switch(control->cond) {
    case COND_NE: 
        bne(cpu, control->disp22, control->a);
        break;
    case COND_ALWAYS:
        b(cpu, control->disp22, control->a);
        break;
    case COND_N:
        bn(cpu, control->disp22, control->a);
        break;
    case COND_E:
        be(cpu, control->disp22, control->a);
        break;
    case COND_LE:
	ble(cpu, control->disp22, control->a);
	break;
    case COND_L:
	bl(cpu, control->disp22, control->a);
	break;
    case COND_G:
	bg(cpu, control->disp22, control->a);
	break;
    case COND_CC:
	bcc(cpu, control->disp22, control->a);
	break;
    default:
        unknownInst(cpu, control);
    }
}

static void processAdd(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        add(cpu, control->rd, control->rs1, control->rs2);
    } else {
        addi(cpu, control->rd, control->rs1, control->simm);
    }

    if (control->op3 == OP3_ADDCC) {
	apply_pending_icc(cpu);
    }
}

static void processAnd(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        and(cpu, control->rd, control->rs1, control->rs2);
    } else {
        andi(cpu, control->rd, control->rs1, control->simm);
    }

    if (control->op3 == OP3_ANDCC) {
	apply_pending_icc(cpu);
    }
}

static void processAndn(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        andn(cpu, control->rd, control->rs1, control->rs2);
    } else {
        andni(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processOr(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        or(cpu, control->rd, control->rs1, control->rs2);
    } else {
        ori(cpu, control->rd, control->rs1, control->simm);
    }

    if (control->op3 == OP3_ORCC) {
	apply_pending_icc(cpu);
    }
}

static void processOrn(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        orn(cpu, control->rd, control->rs1, control->rs2);
    } else {
        orni(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processSub(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        sub(cpu, control->rd, control->rs1, control->rs2);
    } else {
        subi(cpu, control->rd, control->rs1, control->simm);
    }

    if (control->op3 == OP3_SUBCC) {
	apply_pending_icc(cpu);
    }
}

static void processXor(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        xor(cpu, control->rd, control->rs1, control->rs2);
    } else {
        xori(cpu, control->rd, control->rs1, control->simm);
    }

    if (control->op3 == OP3_XORCC) {
	apply_pending_icc(cpu);
    }
}

static void processSll(struct CPU *cpu, struct control_signals *control) { 
    if (control->i == 0) {
	sll(cpu, control->rd, control->rs1, control->rs2);
    } else {
	slli(cpu, control->rd, control->rs1, control->simm & 0x1F);
    }
}

static void processSrl(struct CPU *cpu, struct control_signals *control) { 
    if (control->i == 0) {
	srl(cpu, control->rd, control->rs1, control->rs2);
    } else {
	srli(cpu, control->rd, control->rs1, control->simm & 0x1F);
    }
}

static void processSra(struct CPU *cpu, struct control_signals *control) { 
    if (control->i == 0) {
	sra(cpu, control->rd, control->rs1, control->rs2);
    } else {
	srai(cpu, control->rd, control->rs1, control->simm & 0x1F);
    }
}

static void processTicc(struct CPU *cpu, struct control_signals *control) {
    u32 tt;
    if (control->i == 0) {
	tt = get_reg(cpu, control->rs2) + 128;
    } else {
	tt = control->imm7 + 128;
    }

    switch (control->cond) {
    case COND_ALWAYS:
	cpu->pending_trap = tt;
	break;
    case COND_NE:
	if (!IS_ICC_Z(cpu->psr)) {
	    cpu->pending_trap = tt;
	}
	break;
    case COND_E:
	if (IS_ICC_Z(cpu->psr)) {
	    cpu->pending_trap = tt;
	}
	break;
    default:
	unknownInst(cpu, control);
    }

}

static void processRett(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
	rett(cpu, control->rs1, control->rs2);
    } else {
        retti(cpu, control->rs1, control->simm);
    }
}        

static void processJmpl(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        jmpl(cpu, control->rd, control->rs1, control->rs2);
    } else {
        jmpli(cpu, control->rd, control->rs1, control->simm);
    }
}        

static void processLd(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
	if (control->asi == 0) {
	    ld(cpu, control->rd, control->rs1, control->rs2);
	} else {
	    unknownInst(cpu, control);
	}
    } else {
        ldi(cpu, control->rd, control->rs1, control->simm);
    }
}        

static void processLdsb(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
	if (control->asi == 0) {
	    ldsb(cpu, control->rd, control->rs1, control->rs2);
	} else {
	    unknownInst(cpu, control);
	}
    } else {
        ldsbi(cpu, control->rd, control->rs1, control->simm);
    }
}        

static void processLdsh(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
	if (control->asi == 0) {
	    ldsh(cpu, control->rd, control->rs1, control->rs2);
	} else {
	    unknownInst(cpu, control);
	}
    } else {
        ldshi(cpu, control->rd, control->rs1, control->simm);
    }
}        

static void processLdub(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
	if (control->asi == 0) {
	    ldub(cpu, control->rd, control->rs1, control->rs2);
	} else {
	    unknownInst(cpu, control);
	}
    } else {
        ldubi(cpu, control->rd, control->rs1, control->simm);
    }
}        

static void processLduh(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
	if (control->asi == 0) {
	    lduh(cpu, control->rd, control->rs1, control->rs2);
	} else {
	    unknownInst(cpu, control);
	}
    } else {
        lduhi(cpu, control->rd, control->rs1, control->simm);
    }
}        


static void processLdd(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
	if (control->asi == 0) {
	    ldd(cpu, control->rd, control->rs1, control->rs2);
	} else {
	    unknownInst(cpu, control);
	}
    } else {
        lddi(cpu, control->rd, control->rs1, control->simm);
    }
}        

static void processLdstub(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
	if (control->asi == 0) {
	    ldstub(cpu, control->rd, control->rs1, control->rs2);
	} else {
	    unknownInst(cpu, control);
	}
    } else {
        ldstubi(cpu, control->rd, control->rs1, control->simm);
    }
}        

static void processSt(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        st(cpu, control->rd, control->rs1, control->rs2);
    } else {
        sti(cpu, control->rd, control->rs1, control->simm);
    }        
}

static void processStb(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        stb(cpu, control->rd, control->rs1, control->rs2);
    } else {
        stbi(cpu, control->rd, control->rs1, control->simm);
    }        
}

static void processSth(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        sth(cpu, control->rd, control->rs1, control->rs2);
    } else {
        sthi(cpu, control->rd, control->rs1, control->simm);
    }        
}

static void processStd(struct CPU *cpu, struct control_signals *control) {
    if(control->i == 0) {
        std(cpu, control->rd, control->rs1, control->rs2);
    } else {
        stdi(cpu, control->rd, control->rs1, control->simm);
    }        
}

static void processSave(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0)
	myprintf("save r%u, r%u, r%u\n", control->rs1, control->rs2, control->rd);
    else
	myprintf("savei r%u, %d, r%u\n", control->rs1, control->simm, control->rd);

    u32 new_CWP = (CWP(cpu) + NUM_WINDOWS - 1) % NUM_WINDOWS;
    u32 add_result;

    if (cpu->wim & (1 << new_CWP)) {
	set_pending_trap(cpu, WINDOW_OVERFLOW);
    } else {
	/* We need to use rs1/rs2 from the old CWP but rd from the new one. */
	if(control->i == 0) {
	    add_result = get_reg(cpu, control->rs1) + get_reg(cpu, control->rs2);
	} else {
	    add_result = get_reg(cpu, control->rs1) + control->simm;
	}
	set_cwp(cpu, new_CWP);
	set_reg(cpu, control->rd, add_result);
    }
}

static void processRestore(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0)
	myprintf("restore r%u, r%u, r%u\n", control->rs1, control->rs2, control->rd);
    else
	myprintf("restorei r%u, %d, r%u\n", control->rs1, control->simm, control->rd);

    u32 new_CWP = (CWP(cpu) + 1) % NUM_WINDOWS;
    u32 add_result;

    if (cpu->wim & (1 << new_CWP)) {
	set_pending_trap(cpu, WINDOW_UNDERFLOW);
    } else {
	/* We need to use rs1/rs2 from the old CWP but rd from the new one. */
	if(control->i == 0) {
	    add_result = get_reg(cpu, control->rs1) + get_reg(cpu, control->rs2);
	} else {
	    add_result = get_reg(cpu, control->rs1) + control->simm;
	}
	set_cwp(cpu, new_CWP);
	set_reg(cpu, control->rd, add_result);
    }
}

static void processRdpsr(struct CPU *cpu, struct control_signals *control) {
    if (SUPERVISOR(cpu)) {
	rdpsr(cpu, control->rd);
    } else {
	set_pending_trap(cpu, PRIVILEGED_INSTRUCTION);
    }
}

static void processWrpsr(struct CPU *cpu, struct control_signals *control) {
    if (SUPERVISOR(cpu)) {
	if (control->i == 0) {
	    wrpsr(cpu, control->rs1, control->rs2);
	} else {
	    wrpsri(cpu, control->rs1, control->simm);
	}
    } else {
	myprintf("Current psr: 0x%x\n", cpu->psr);
	set_pending_trap(cpu, PRIVILEGED_INSTRUCTION);
    }
}

static void processRdwim(struct CPU *cpu, struct control_signals *control) {
    if (SUPERVISOR(cpu)) {
	rdwim(cpu, control->rd);
    } else {
	set_pending_trap(cpu, PRIVILEGED_INSTRUCTION);
    }
}

static void processWrwim(struct CPU *cpu, struct control_signals *control) {
    if (SUPERVISOR(cpu)) {
	if (control->i == 0) {
	    wrwim(cpu, control->rs1, control->rs2);
	} else {
	    wrwimi(cpu, control->rs1, control->simm);
	}
    } else {
	set_pending_trap(cpu, PRIVILEGED_INSTRUCTION);
    }
}

static void processRdtbr(struct CPU *cpu, struct control_signals *control) {
    if (SUPERVISOR(cpu)) {
	rdtbr(cpu, control->rd);
    } else {
	set_pending_trap(cpu, PRIVILEGED_INSTRUCTION);
    }
}

static void processWrtbr(struct CPU *cpu, struct control_signals *control) {
    if (SUPERVISOR(cpu)) {
	if (control->i == 0) {
	    wrtbr(cpu, control->rs1, control->rs2);
	} else {
	    wrtbri(cpu, control->rs1, control->simm);
	}
    } else {
	set_pending_trap(cpu, PRIVILEGED_INSTRUCTION);
    }
}

static void processRdasr(struct CPU *cpu, struct control_signals *control) {
    if (control->rs1 == 0) {
	rdy(cpu, control->rd);
    } else {
	unknownInst(cpu, control);
    }
}

static void processWrasr(struct CPU *cpu, struct control_signals *control) {
    if (control->rd == 0) {
	if (control->i == 0) {
	    wry(cpu, control->rs1, control->rs2);
	} else {
	    wryi(cpu, control->rs1, control->simm);
	}
    } else {
	unknownInst(cpu, control);
    }
}

static void processMulscc(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0) {
	mulscc(cpu, control->rd, control->rs1, control->rs2);
    } else {
	mulscci(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processUmul(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0) {
	umul(cpu, control->rd, control->rs1, control->rs2);
    } else {
	umuli(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processSmul(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0) {
	smul(cpu, control->rd, control->rs1, control->rs2);
    } else {
	smuli(cpu, control->rd, control->rs1, control->simm);
    }
}
static void processUmulcc(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0) {
	umulcc(cpu, control->rd, control->rs1, control->rs2);
    } else {
	umulcci(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processSmulcc(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0) {
	smulcc(cpu, control->rd, control->rs1, control->rs2);
    } else {
	smulcci(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processUdiv(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0) {
	udiv(cpu, control->rd, control->rs1, control->rs2);
    } else {
	udivi(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processSdiv(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0) {
	sdiv(cpu, control->rd, control->rs1, control->rs2);
    } else {
	sdivi(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processUdivcc(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0) {
	udivcc(cpu, control->rd, control->rs1, control->rs2);
    } else {
	udivcci(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processSdivcc(struct CPU *cpu, struct control_signals *control) {
    if (control->i == 0) {
	sdivcc(cpu, control->rd, control->rs1, control->rs2);
    } else {
	sdivcci(cpu, control->rd, control->rs1, control->simm);
    }
}

static void processFormat1(struct CPU *cpu, struct control_signals *control) {
    call(cpu, control->disp30);    
}

static void processFormat2(struct CPU *cpu, struct control_signals *control) {
    switch(control->op2) {
    case OP2_BRANCH:
        processBranch(cpu, control);
        break;
    case OP2_SETHI:
        sethi(cpu, control->rd, control->imm);
        break;
    case OP2_NOP:
	myprintf("NOP%s","\n");
	break;
    default:
        unknownInst(cpu, control);
    }
}

static void processFormat3Mem(struct CPU *cpu, struct control_signals *control) {
    switch(control->op3) {
    case OP3_LD:
        processLd(cpu, control);
        break;
    case OP3_LDUB:
        processLdub(cpu, control);
	break;
    case OP3_LDUH:
        processLduh(cpu, control);
	break;
    case OP3_LDSB:
        processLdsb(cpu, control);
	break;
    case OP3_LDSH:
        processLdsh(cpu, control);
	break;
    case OP3_LDD:
        processLdd(cpu, control);
        break;
    case OP3_LDSTUB:
	processLdstub(cpu, control);
	break;
    case OP3_STB:
        processStb(cpu, control);
        break;
    case OP3_STH:
        processSth(cpu, control);
        break;
    case OP3_ST:
        processSt(cpu, control);
        break;	
    case OP3_STD:
        processStd(cpu, control);
        break;
    default:
        unknownInst(cpu, control);
    }
}

static void processFormat3Alu(struct CPU *cpu, struct control_signals *control) {
    switch(control->op3) {
    case OP3_ADD:
    case OP3_ADDCC:
        processAdd(cpu, control);
        break;
    case OP3_AND:
    case OP3_ANDCC:
	processAnd(cpu, control);
	break;
    case OP3_ANDN:
	processAndn(cpu, control);
	break;
    case OP3_JMPL:
        processJmpl(cpu, control);
        break;
    case OP3_MULSCC:
	processMulscc(cpu, control);
	break;
    case OP3_OR:
    case OP3_ORCC:
        processOr(cpu, control);
        break;
    case OP3_ORN:
        processOrn(cpu, control);
        break;
    case OP3_SAVE:
	processSave(cpu, control);
	break;
    case OP3_SLL:
	processSll(cpu, control);
	break;
    case OP3_SRA:
	processSra(cpu, control);
	break;
    case OP3_SRL:
	processSrl(cpu, control);
	break;
    case OP3_SUB:
    case OP3_SUBCC:
        processSub(cpu, control);
        break;
    case OP3_RDASR:
	processRdasr(cpu, control);
	break;
    case OP3_RDPSR:
	processRdpsr(cpu, control);
	break;
    case OP3_RDTBR:
	processRdtbr(cpu, control);
	break;
    case OP3_RDWIM:
	processRdwim(cpu, control);
	break;
    case OP3_RESTORE:
	processRestore(cpu, control);
	break;
    case OP3_RETT:
	processRett(cpu, control);
	break;
    case OP3_UMUL:
	processUmul(cpu, control);
	break;
    case OP3_UMULCC:
	processUmulcc(cpu, control);
	break;
    case OP3_SMUL:
	processSmul(cpu, control);
	break;
    case OP3_SMULCC:
	processSmulcc(cpu, control);
	break;
    case OP3_UDIV:
	processUdiv(cpu, control);
	break;
    case OP3_UDIVCC:
	processUdivcc(cpu, control);
	break;
    case OP3_SDIV:
	processSdiv(cpu, control);
	break;
    case OP3_SDIVCC:
	processSdivcc(cpu, control);
	break;
    case OP3_WRASR:
	processWrasr(cpu, control);
	break;
    case OP3_WRPSR:
	processWrpsr(cpu, control);
	break;
    case OP3_WRTBR:
	processWrtbr(cpu, control);
	break;
    case OP3_WRWIM:
	processWrwim(cpu, control);
	break;
    case OP3_XOR:
    case OP3_XORCC:
        processXor(cpu, control);
        break;
    case OP3_TICC:
	processTicc(cpu, control);
	break;

    default:
        unknownInst(cpu, control);
    }
}

static bool execute(struct CPU *cpu, struct control_signals *control) {    
    myprintf("0x%x ", cpu->pc);

    // writes to reg 0 are ignored and reads should always be 0
    set_reg(cpu, 0, 0);

    // Todo: How are annul'd traps handled?
    if (cpu->pending_annul) {
	cpu->pending_annul = 0;
    } else {
	switch(control->op) {
	case OP_FORMAT_1:
	    processFormat1(cpu, control);
	    break;
	case OP_FORMAT_2:
	    processFormat2(cpu, control);
	    break;
	case OP_FORMAT_3_ALU:
	    processFormat3Alu(cpu, control);
	    break;
	case OP_FORMAT_3_MEM:
	    processFormat3Mem(cpu, control);
	    break;
	default:
	    unknownInst(cpu, control);
	}
    }
    set_reg(cpu, 0, 0);

    if(cpu->pending_trap) {
	myprintf("Trap: %u\n",cpu->pending_trap);
	
        return false;
    }

    cpu->pc = cpu->npc;
    if(cpu->branch_pc != (u32) -1) {
        cpu->npc = cpu->branch_pc;
        cpu->branch_pc = (u32) -1;
    } else {
        cpu->npc += 4;
    }

    return true;
}

bool cpu_exec_inst(struct CPU *cpu) {
    struct control_signals control;
    u32 inst = fetch(cpu);
    decode_control_signals(inst, &control);

    if(cpu->pending_trap) {
        return false;
    }
    
    // second part of decode and write back also
    execute(cpu, &control);

    if (cpu->pending_trap) {
	//handle_trap(cpu);
	return false;
    }
    return true;
}

