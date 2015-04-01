#include "priv.h"

void unhandledTrap(struct CPU *cpu) {
    myprintf("Unhandled trap. tt=%u\n",cpu->pending_trap);
    exit(-1);
}

void handleWindowOverflow(struct CPU *cpu) {
    /* Rotate WIM bit right */
    u32 wim_mask = (1 << NUM_WINDOWS) - 1;
    u32 new_wim = cpu->wim & wim_mask;
    new_wim = new_wim >> 1;
    new_wim &= wim_mask;
    
    /* if it's 0, then we've wrapped around */
    if (new_wim == 0)
	new_wim |= 1 << (NUM_WINDOWS-1);
    
    /* Disable WIM traps for now */
    cpu->wim = 0 ;
    
    /* Dump the registers from the now invalid window onto the stack */

    /* decrement the window pointer so we're looking at the window to backup */
    decrement_cwp(cpu);
   
    u32 sp = get_reg(cpu, SP);

    if (sp != 0)
    {
	guestStore(sp + 0, get_reg(cpu, L0));
	guestStore(sp + 4, get_reg(cpu, L1));
	guestStore(sp + 8, get_reg(cpu, L2));
	guestStore(sp + 12, get_reg(cpu, L3));
	guestStore(sp + 16, get_reg(cpu, L4));
	guestStore(sp + 20, get_reg(cpu, L5));
	guestStore(sp + 24, get_reg(cpu, L6));
	guestStore(sp + 28, get_reg(cpu, L7));

	guestStore(sp + 32, get_reg(cpu, I0));
	guestStore(sp + 36, get_reg(cpu, I1));
	guestStore(sp + 40, get_reg(cpu, I2));
	guestStore(sp + 44, get_reg(cpu, I3));
	guestStore(sp + 48, get_reg(cpu, I4));
	guestStore(sp + 52, get_reg(cpu, I5));
	guestStore(sp + 56, get_reg(cpu, I6));
	guestStore(sp + 60, get_reg(cpu, I7));
    }

    /* increment CWP */
    increment_cwp(cpu);

    /* Set WIM back on w/ new value */
    cpu->wim = new_wim;
}

void handleWindowUnderflow(struct CPU *cpu) {
    /* Rotate WIM bit lef */
    u32 wim_mask = (1 << NUM_WINDOWS) - 1;
    u32 new_wim = cpu->wim & wim_mask;    
    new_wim = new_wim << 1;
    new_wim &= wim_mask;
    
    
    /* if it's 0, then we've wrapped around */
    if (new_wim == 0)
	new_wim |= 1;
    
    /* Disable WIM traps for now */
    cpu->wim = 0 ;
    
    /* Dump the registers from the now invalid window onto the stack */

    /* Double increment the CWP */
    increment_cwp(cpu);
    increment_cwp(cpu);

    u32 sp = get_reg(cpu, SP);

    assert(sp != 0);

    u32 dest;
    guestLoad(sp + 0, &dest); set_reg(cpu, L0, dest);
    guestLoad(sp + 4, &dest); set_reg(cpu, L1, dest);
    guestLoad(sp + 8, &dest); set_reg(cpu, L2, dest);
    guestLoad(sp + 12, &dest); set_reg(cpu, L3, dest);
    guestLoad(sp + 16, &dest); set_reg(cpu, L4, dest);
    guestLoad(sp + 20, &dest); set_reg(cpu, L5, dest);
    guestLoad(sp + 24, &dest); set_reg(cpu, L6, dest);
    guestLoad(sp + 28, &dest); set_reg(cpu, L7, dest);

    guestLoad(sp + 32, &dest); set_reg(cpu, I0, dest);
    guestLoad(sp + 36, &dest); set_reg(cpu, I1, dest);
    guestLoad(sp + 40, &dest); set_reg(cpu, I2, dest);
    guestLoad(sp + 44, &dest); set_reg(cpu, I3, dest);
    guestLoad(sp + 48, &dest); set_reg(cpu, I4, dest);
    guestLoad(sp + 52, &dest); set_reg(cpu, I5, dest);
    guestLoad(sp + 56, &dest); set_reg(cpu, I6, dest);
    guestLoad(sp + 60, &dest); set_reg(cpu, I7, dest);

    /* Double decrement CWP */
    decrement_cwp(cpu);
    decrement_cwp(cpu);    

    /* Set WIM back on w/ new value */
    cpu->wim = new_wim;
}

void handle_trap(struct CPU *cpu) {
    trapprintf("Handling trap %u\n", cpu->pending_trap);

    /* preserve supervisor state in PS */
    cpu->psr &= ~PSR_PS_MASK;
    if (cpu->psr & PSR_S_MASK) {
	cpu->psr |= 1 << 6;
    }

    /* Enable Supervisor */
    cpu->psr |= 1 << 7; 

    /* This doens't test for over/under flow */
    decrement_cwp(cpu);
    
    /* Disable traps */
    cpu->psr &= ~PSR_ET_MASK;

    /* Save PC and NPC to new window's locals */
    set_reg(cpu, L1, cpu->pc);
    set_reg(cpu, L2, cpu->npc);

    /* Set tt in tbr */
    u32 tt_mask = 0xFF0;
    u32 tt = cpu->pending_trap;
    
    cpu->tbr &= ~tt_mask;
    assert (((tt << 4) & ~tt_mask) == 0);	
    
    cpu->tbr |= (tt << 4);

    if (HANDLE_TRAPS_IN_SIM)
    {
	switch(cpu->pending_trap) {
	case WINDOW_OVERFLOW:
	    handleWindowOverflow(cpu);
	    break;
	case WINDOW_UNDERFLOW:
	    handleWindowUnderflow(cpu);
	    break;
	default:
	    unhandledTrap(cpu);
	}
	
	/* RETT usually handles this */
	/* This doens't test for over/under flow */
	increment_cwp(cpu);

    }
    else
    {
	u32 trap_handler = cpu->tbr & ~0xF;

	/* FIXME  if reset, goto addr 0 */

	cpu->pc = trap_handler;
	cpu->npc = trap_handler + 4;

    }

    /* Remove our internal do trap state. */
    cpu->pending_trap = 0;
}



