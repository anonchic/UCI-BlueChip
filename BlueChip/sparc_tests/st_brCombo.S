/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: brCombo.s,v 1.3 91/08/12 08:49:27 rudi Exp $"
.align 8
.seg "text"
#include "assy_macros.h"

/*******************************************************************************


	SYNOPSIS:

	This diagnostic tests combinations involving branches.

	ERROR REPORTING:

	This diagnostic incorporates the error reporting standard.
	All errors are logged in memory beginning at log_area.  Each half
	word (after the first half word) contains the error number that
	falied.  Find the call to:

		check_error(n ...)

	in this source. This is the point at which the error was detected.

	Complete documentation of the error reporting standard can be found
	on include/err_rprt.doc


*******************************************************************************/

#define ERR_TEMP	%g7


/******************************************************************************

		TRAP TABLE AND ERROR REPORTING

*******************************************************************************/

#include "err_incr.h"
#define ERR_RET	%r15		/* make %o7 the error return reg */
#define ERR_RET_NUM 15

#include "err_macro.h"

new_init(brCombo)


#include "cmbtt.S"

#include "err_subr.S"


/******************************************************************************

		ERROR REPORTING

******************************************************************************/


/******************************************************************************

	STARTUP ROUTINE

*******************************************************************************/

#include "su_super.S"

/*******************************************************************************

	MAIN PROGRAM

*******************************************************************************/

main:

		save	%g0, -64, %sp


/*******************************************************************************

	BRANCHES WHICH ANNUL SINGLE-CYCLE ALU INSTRUCTIONS

*******************************************************************************/
		
annul_alu_u:
		mov	-1,%g6
		mov	1, %l0
		mov	2, %l1
		mov	0, %l2
		mov	-321, %l4

		cmp	%g0, %g0
		bne,a	neverGetHere
		addcc	%l0, %l1, %l2		! ANNULLED

		be	1f			! check that Z bit is still set
		nop
		check_error(NEXTE, ba)		! shouldn't be here
		b	stop_end
		nop
	1:
		cmp	%l2, 0			! check that %l2 is still zero
		check_error(NEXTE, bne)
		cmp	%l4, -321		! check that target was annulled
		check_error(NEXTE, bne)

		! REPEAT with branch taken

annul_alu_t:
		mov	0, %l2

		tst	%g0			! set Z bit
		b,a	1f
		addcc	%l0, %l1, %l2		! ANNULLED

		check_error(NEXTE, ba)		! shouldn't get here
		b	stop_end
		nop
	1:
		be	1f			! check that Z bit is still set
		nop
		check_error(NEXTE, ba)		! shouldn't be here
		b	stop_end
		nop
	1:
		cmp	%l2, 0			! check that %l2 is still zero
		check_error(NEXTE, bne)
		

/*******************************************************************************

	BRANCHES WHICH ANNUL LOADS

*******************************************************************************/

		.seg "data"
		MEM_VALUE = 5
		MEM_VALUE_2 = -5
		.align	8
notZero:	.word MEM_VALUE
		.word MEM_VALUE_2
		.seg "text"


annul_ld_u:

		set	notZero, %l2	! address to not load from

		mov	-354, %l4	! annulled target would
					! change %l4 if it is executed

		mov	-69, %l0	! register to not load
		cmp	%g0, %g0
		bne,a	neverGetHere
		ld	[%l2 + 1], %l0	! ANNULLED (MISALIGNED)

		cmp	%l0, -69	! make sure delay-slot was annulled
		check_error(NEXTE, bne)
		cmp	%l4, -354	! make sure target was annulled
		check_error(NEXTE, bne)

			! do the same with branch taken

annul_ld_t:
		b,a	1f
		ld	[%l2], %l0	! ANNULLED
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
		cmp	%l0, -69	! make sure delay-slot was annulled
		check_error(NEXTE, bne)

	! ANNUL STORES

annul_st_u:
		mov	-6, %l1
		cmp	%g0, 1
		be,a	neverGetHere
		st	%g0, [%l2 + 1]	! ANNULLED (MISALIGNED)
		ld	[%l2], %l1	! make sure store didn't happen
		cmp	%l1, MEM_VALUE
		check_error(NEXTE, bne)	! error if memory value was altered
		cmp	%l4, -354	! make sure target was annulled
		check_error(NEXTE, bne)

			! do the same with branch taken
		
annul_st_t:
		b,a	1f
		st	%g0, [%l2]	! ANNULLED (NO EXCEPTION)
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop

	1:
		ld	[%l2], %l1	! make sure store didn't happen
		cmp	%l1, MEM_VALUE
		check_error(NEXTE, bne)	! error if memory value was altered

annul_stm_t:
		b,a	1f
		st	%g0, [%l2 + 1]	! ANNULLED (MISALIGNED)
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop

	1:
		ld	[%l2], %l1	! make sure store didn't happen
		cmp	%l1, MEM_VALUE
		check_error(NEXTE, bne)	! error if memory value was altered

/*******************************************************************************

	ANNUL LDSTs

*******************************************************************************/

annul_ldst_u:

		set	notZero, %l2	! address to not load from

		mov	-354, %l4	! annulled target would
					! change %l4 if it is executed

		mov	-69, %l0	! register to not load
		cmp	%g0, %g0
		bne,a	neverGetHere
		ldstub	[%l2], %l0	! ANNULLED

		cmp	%l0, -69	! make sure delay-slot was annulled
		check_error(NEXTE, bne)	! check that load did not occur
		ld	[%l2], %l1	! check that store did not occur
		cmp	%l1, MEM_VALUE
		check_error(NEXTE, bne)
		cmp	%l4, -354	! make sure target was annulled
		check_error(NEXTE, bne)

			! do the same with branch taken

annul_ldst_t:
		b,a	1f
		ldstub	[%l2 + 1], %l0	! ANNULLED (MISALIGNED)
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
		cmp	%l0, -69	! make sure delay-slot was annulled
		check_error(NEXTE, bne)
		ld	[%l2], %l1	! check that store did not occur
		cmp	%l1, MEM_VALUE
		check_error(NEXTE, bne)

/*******************************************************************************

	! ANNUL LDF		NOTE:
	!			whether an LDF is annulled or not cannot
	!			be properly checked on the DAISY simulator
	!			because we aren't modeling the FPU.
	!			The following tests MUST to be monitored
	!			by hand.

*******************************************************************************/

annul_ldf_u:
		! let's clear the EF bit so that ldf would generate
		! fp_disabled trap

		rd	%psr, %l0
		set	0x1000, %l1
		andn	%l0, %l1, %l0
		wr	%l0, %psr
		nop
		nop
		nop


		set	notZero, %l2	! address to not load from

		mov	-354, %l4	! annulled target would
					! change %l4 if it is executed

		mov	-69, %l0	! register to not load
		cmp	%g0, %g0
		bne,a	neverGetHere
		ldd	[%l2], %f0	! ANNULLED (FP_DISABLED)
					
					! can't check on Daisy

		cmp	%l4, -354	! make sure target was annulled
		check_error(NEXTE, bne)

			! do the same with branch taken

annul_ldf_t:
		b,a	1f
		ldd	[%l2 + 1], %f0	! ANNULLED (FP_DISABLED) (MISALIGNED)
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
/*******************************************************************************

	ANNUL BRANCHES

*******************************************************************************/

! Note: annul_br_u test case now found in illegal_branches.s

annul_br_t:
		cmp	%g0, %g0	! set Z condition code bit
		b,a	1f
		be,a	neverGetHere	! ANNULLED
		mov	-1, %l4		! should not be executed
		
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
		cmp	%g0, -69	! lets be sure that this instruction
					! IS executed !
		check_error(NEXTE, be)
		cmp	%l4, -354	! make sure that the delay-slot of
		check_error(NEXTE, bne)	! the annulled branch was not executed

! Note: annul_fbr_u test case now found in illegal_branches.s

annul_fbr_t:
		cmp	%g0, %g0	! set Z condition code bit
		b,a	1f
		fbu,a	neverGetHere	! ANNULLED (FP_DISABLED)
		mov	-1, %l4		! should not be executed
		
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
		cmp	%g0, -69	! lets be sure that this instruction
					! IS executed !
		check_error(NEXTE, be)
		cmp	%l4, -354	! make sure that the delay-slot of
		check_error(NEXTE, bne)	! the annulled branch was not executed


/*******************************************************************************

	ANNUL JUMPS

*******************************************************************************/

/**********************

	This test creates an illegal CTI couple

annul_jmp_u:
		set	neverGetHere, %l2	! address to not jump to

		mov	-354, %l4	! annulled target would
					! change %l4 if it is executed

		cmp	%g0, %g0
		bne,a	neverGetHere
		jmp	%l2 + 1		! ANNULLED (MISALIGNED)

		cmp	%l4, -354	! make sure target was annulled
		check_error(NEXTE, bne)

			! do the same with branch taken

*******************/

annul_jmp_t:
		cmp	%g0, %g0	! set Z condition code
		b,a	1f
		jmp	%l2		! ANNULLED
		mov	-1, %l4		! should not be executed

		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
		cmp	%g0, -69	! lets be sure that this instruction
					! IS executed !
		check_error(NEXTE, be)
		cmp	%l4, -354	! make sure delay slot of annulled
		check_error(NEXTE, bne)	! jump is not executed.


/*******************************************************************************

	ANNUL SAVE

*******************************************************************************/

		! set WIM such that a save now would cause an overflow

		rd	%psr, %l0
		and	%l0, 0x1f, %l0	! mask for CWP
		sub	%l0, 1, %l0
		tst	%l0
		bl	1f
		mov	1, %l1
		b	2f
		sll	%l1, %l0, %l1	! new WIM
	1:	mov	6, %l0
		sll	%l1, %l0, %l1	! new WIM
	2:
		rd	%wim, %l5	! save old WIM
		wr	%l1, %wim

annul_save_u:
		mov	-354, %l4	! annulled target would
					! change %l4 if it is executed

		cmp	%g0, %g0
		bne,a	neverGetHere
		save			! ANNULLED

		cmp	%l4, -354	! make sure delay-slot was annulled
		check_error(NEXTE, bne)	! also makes sure target was annuled

			! do the same with branch taken

annul_save_t:
		b,a	1f
		save			! ANNULLED (OVERFLOW)
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
		cmp	%l4, -354	! make sure delay-slot was annulled
		check_error(NEXTE, bne)

/*******************************************************************************

	ANNUL RESTORE

*******************************************************************************/

		! set WIM such that a restore now would cause an underflow

		rd	%psr, %l0
		and	%l0, 0x1f, %l0	! mask for CWP
		add	%l0, 1, %l0	! WIM must point at current window + 1
		cmp	%l0, 6		! check for wrap-around
		bg	2f
		mov	1, %l1
		sll	%l1, %l0, %l1	! new WIM
	2:
		wr	%l1, %wim

annul_restore_u:
		mov	-354, %l4	! annulled target would
					! change %l4 if it is executed

		cmp	%g0, %g0
		bne,a	neverGetHere
		restore			! ANNULLED (UNDERFLOW)

		cmp	%l4, -354	! make sure delay-slot was annulled
		check_error(NEXTE, bne)	! also makes sure target was annuled

			! do the same with branch taken

annul_restore_t:
		b,a	1f
		restore			! ANNULLED
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
		cmp	%l4, -354	! make sure delay-slot was annulled
		check_error(NEXTE, bne)

		wr	%l5, %wim	! restore old WIM
		nop
		nop
		nop


/*******************************************************************************

	ANNUL TICC

*******************************************************************************/

annul_ticc_u:
		mov	-354, %l4	! annulled target would
					! change %l4 if it is executed

		cmp	%g0, %g0
		bne,a	neverGetHere
		te	10		! ANNULLED
					! if the trap IS taken, the trap
					! handler will log the error.

		cmp	%l4, -354	! make sure target was annulled
		check_error(NEXTE, bne)

			! do the same with branch taken

annul_ticc_t:
		b,a	1f
		t	10		! ANNULED
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
		cmp	%l4, -354	! make sure delay-slot was annulled
		check_error(NEXTE, bne)


/*******************************************************************************

	ANNUL RETT


	These test cases REMOVED because it is now illegal to precede RETT
	with anything but JMP

annul_rett_u:
		mov	-354, %l4	! annulled target would
					! change %l4 if it is executed
		rd	%psr, %l0
		andn	%l0, 0x20, %l0	! clear the ET bit
		wr	%l0, %psr

		cmp	%g0, %g0
		set	neverGetHere, %l5
		bne,a	neverGetHere
		rett	%l5		! ANNULED

		cmp	%l4, -354	! make sure that window hasn't changed
		check_error(NEXTE, bne)	! also makes sure target was annuled
		rd	%psr, %l1
		andcc	%l0, 0x20, %g0	! check that ET is still clear
		check_error(NEXTE, bne)

			! do the same with branch taken

annul_rett_t:
		rd	%psr, %l0
		andn	%l0, 0x20, %l0	! clear the ET bit
		wr	%l0, %psr

		set	neverGetHere, %l5
		b,a	1f
		rett	%l5		! ANNULED
		mov	-1, %l4		! in delay slot of RETT - not executed
		check_error(NEXTE, ba)	! it is an error to be here
		b	stop_end
		nop
	1:
		cmp	%l4, -354	! make sure that window hasn't changed
		check_error(NEXTE, bne)	! also makes sure target was annuled
		rd	%psr, %l1
		andcc	%l1, 0x20, %g0	! check that ET is still clear
		check_error(NEXTE, bne)
					! set the ET bit again
		or	%l1, 0x20, %l1
		wr	%l1, %psr

*******************************************************************************/


/*******************************************************************************

	TAKEN BRANCHES WITH VARIOUS INSTRUCTIONS IN THE DELAY SLOT
	AND BRANCH TARGET ALSO A BRANCH

*******************************************************************************/

br_alu:	
		mov	1, %l0
		mov	2, %l1
		mov	0, %l2
		mov	-5, %y		! write Y, read later
		mov	0, %l3
		mov	-984, %l4

		cmp	%g0, %g0	! set Z bit

		be	1f
		addcc	%l0, %l1, %l2
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	1:
		bn	neverGetHere	! untaken
		rd	%y, %l3
		mov	%l3, %l5
		bne	2f		! test that Z not set. should be taken
		nop
		check_error(NEXTE, ba)	! shouldn't be here
		b	stop_end
		nop

	2:
		cmp	%l5, -5		! check that RDY worked in delay slot
		check_error(NEXTE, bne)
		cmp	%l2, 3		! check that addcc wrote proper result
		check_error(NEXTE, bne)
		cmp	%l4, -984	! check that bn target was annulled
		check_error(NEXTE, bne)


		! test LOAD in delay slot of branch (taken, target also taken )
br_ld:	

		.seg "data"
		MEM_BYTE = -4
memLoc:		.byte	MEM_BYTE
		.seg "text"

		set	memLoc, %l0
		mov	-984, %l4

		cmp	%g0, %g0	! set Z bit

		be	1f
		ldsb	[%l0], %l2
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	1:
		be	2f		! taken
		bn	neverGetHere	! untaken
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	2:
		cmp	%l2, MEM_BYTE	! check that ldsb worked
		check_error(NEXTE, bne)
		cmp	%l4, -984	! check that bn target was annulled
		check_error(NEXTE, bne)


		! test STORE in delay slot of branch (taken, target untaken )

br_st:	

		set	memLoc, %l0
		mov	-984, %l4
		mov	-5, %l1

		cmp	%g0, %g0	! set Z bit

		be	1f
		stb	%l1, [%l0]
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	1:
		bn	neverGetHere	! untaken
		nop

		ldsb	[%l0], %l2
		cmp	%l2, -5		! check that ldsb worked
		check_error(NEXTE, bne)
		cmp	%l4, -984	! check that bn target was annulled
		check_error(NEXTE, bne)


		! test ldd in delay slot of branch (taken, target also taken )
		! NOTE:
		!	The affect of ldd cannot be tested without a float
		!	unit. The following case MUST be observed by hand.

br_ldd:	
		! need to enable floating-point again (set EF)

		rd	%psr, %l0
		set	0x1000, %l1
		or	%l0, %l1, %l0
		wr	%l0, %psr

#define MEM_DOUBLE  0r1.5
		.seg "data"
		.align	8
fmemLoc:	.double	MEM_DOUBLE
		.seg "text"

		set	fmemLoc, %l0
		mov	-984, %l4

		cmp	%g0, %g0	! set Z bit

		be	1f
		ldd	[%l0], %l0

		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	1:
		be	2f		! taken
		bn	neverGetHere	! untaken, has no affect
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	2:
		cmp	%l4, -984	! check that bn target was annulled
		check_error(NEXTE, bne)

		! test std in delay slot of branch (taken, target untaken )

br_std:	

		set	fmemLoc, %l0
		mov	-984, %l4
		mov	-5, %l1

		cmp	%g0, %g0	! set Z bit

		be	1f
		std	%l0, [%l0]
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	1:
		bn	neverGetHere	! untaken
		nop

		cmp	%l4, -984	! check that bn target was annulled
		check_error(NEXTE, bne)


		! test jmp in delay slot of branch
		! (branch target is an untaken branch)

br_jmp_u:	

		mov	-984, %l4
		mov	-54, %l1
		mov	-345, %l2

		set	2f, %l0		! jump target
		cmp	%g0, %g0	! set Z bit

		be	1f
		jmp	%l0

		mov	0, %l1		! should NOT be executed
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	1:
		bn	neverGetHere	! untaken but executed
		mov	0, %l2		! should NOT be executed
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	2:
		cmp	%l2, -345	! check that instr. after branch
					! target was not executed
		check_error(NEXTE, bne)
		cmp	%l4, -984	! check that bn target was annulled
		check_error(NEXTE, bne)
		cmp	%l1, -54	! check that instr. after jump NOT exec.
		check_error(NEXTE, bne)


		! test jmp in delay slot of branch
		! (branch target is a taken branch)

br_jmp_t:	

		mov	-54, %l1
		mov	-345, %l2
		mov	452, %l3
		mov	-98, %l5

		set	2f, %l0		! jump target
		cmp	%g0, %g0	! set Z bit

		be	1f
		jmp	%l0

		mov	0, %l1		! should NOT be executed
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	1:
		be	3f		! taken
		mov	0, %l2		! should NOT be executed
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	2:				! jump target
		mov	315, %l3	! EXECUTED
		mov	0, %l5		! NOT exec.
		check_error(NEXTE, ba)	! should NOT be here
		b	stop_end
		nop

	3:
		cmp	%l1, -54	! check that instr. after jump NOT exec.
		check_error(NEXTE, bne)
		cmp	%l2, -345	! check that instr. after branch
					! target was not executed
		check_error(NEXTE, bne)
		cmp	%l3, 315	! check that jump target was executed
		check_error(NEXTE, bne)
		cmp	%l4, -984	! check that bn target was annulled
		check_error(NEXTE, bne)
		cmp	%l5, -98	! check that instruction after
					! jump target was not executed
		check_error(NEXTE, bne)


/*******************************************************************************

		 test RETT in delay slot of branch
		 (branch target is an untaken branch)

		 These test cases removed because it is now illegal to precede
		 RETT with anything but a JMP instruction

br_rett_u:	

		! must disable traps
		rd	%psr, %l0
		andn	%l0, 0x20 ,%l0	! clear ET bit
		wr	%l0, %psr

		mov	-984, %l4
		mov	-54, %l1
		mov	-345, %l2

		save			! enter next window,
					! expect RETT to restore.
		set	2f, %l0		! RETT target
		cmp	%g0, %g0	! set Z bit

		be	1f
		rett	%l0		! restore window
					! copy pS into S
					! enable traps

		mov	0, %l1		! should NOT be executed
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	1:
		bn	neverGetHere	! untaken but executed
		mov	0, %l2		! should NOT be executed
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	2:	! RETT target, should be back in original window
		cmp	%l2, -345	! check that instr. after branch
					! target was not executed
		check_error(NEXTE, bne)
		cmp	%l4, -984	! check that bn target was annulled
		check_error(NEXTE, bne)
		cmp	%l1, -54	! check that instr. after jump NOT exec.
		check_error(NEXTE, bne)


		! test RETT in delay slot of branch
		! (branch target is a taken branch)

br_rett_t:	
		! must disable traps again
		rd	%psr, %l0
		andn	%l0, 0x20 ,%l0	! clear ET bit
		wr	%l0, %psr

		mov	-54, %l1
		mov	-345, %l2
		mov	452, %l3
		mov	-98, %l5

		save			! expect RETT to restore
		set	2f, %l0		! jump target
		cmp	%g0, %g0	! set Z bit

		be	1f
		rett	%l0		! restore window
					! copy pS into S
					! enable traps

		mov	0, %l1		! should NOT be executed
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	1:
		be	3f		! taken
		mov	0, %l2		! should NOT be executed
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop

	2:				! RETT target
		mov	315, %l3	! EXECUTED
		mov	0, %l5		! NOT exec.
		check_error(NEXTE, ba)	! should NOT be here
		b	stop_end
		nop

	3:
		cmp	%l1, -54	! check that instr. after RETT NOT exec.
		check_error(NEXTE, bne)
		cmp	%l2, -345	! check that instr. after branch
					! target was not executed
		check_error(NEXTE, bne)
		cmp	%l3, 315	! check that jump target was executed
		check_error(NEXTE, bne)
		cmp	%l4, -984	! check that bn target was annulled
		check_error(NEXTE, bne)
		cmp	%l5, -98	! check that instruction after
					! jump target was not executed
		check_error(NEXTE, bne)

*******************************************************************************/


/*******************************************************************************

	BRANCH / BRANCH COMBINATIONS - TARGET OF FIRST BRANCH ALSO A BRANCH
					FIRST BRANCH TAKEN

*******************************************************************************/

br_br_ttu:
		mov	-34, %l0
		mov	-44, %l1
		mov	12, %l2
		mov	99, %l4
		cmp	%g0, %g0

		be	1f		! TAKEN
		be	2f		! TAKEN

		mov	0, %l0
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop
	
	1:	bn	neverGetHere	! UNTAKEN
		mov	-18, %l1	! NOT EXECUTED
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop
	
	2:
		mov	0, %l2
		cmp	%l0, -34
		check_error(NEXTE, bne)
		cmp	%l4, 99
		check_error(NEXTE, bne)
		cmp	%l1, -44
		check_error(NEXTE, bne)
		cmp	%l2, 0
		check_error(NEXTE, bne)


br_br_ttt:
		mov	-34, %l0
		mov	-44, %l1
		mov	12, %l2
		mov	-9, %l3
		mov	99, %l4
		mov	-21, %l5
		cmp	%g0, %g0

		be	1f		! TAKEN
		be	2f		! TAKEN

		mov	0, %l0
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop
	
	1:	be	3f		! TAKEN
		mov	-18, %l1	! NOT EXECUTED
		check_error(NEXTE, ba)	! shouldn't get here
		b	stop_end
		nop
	
	2:	mov	0, %l2		! EXECUTED
		mov	0, %l3		! NOT EXECUTED
		check_error(NEXTE, ba)	! shouldn't be here
		b	stop_end
		nop

	3:
		mov	-25, %l5		! EXECUTED
		cmp	%l0, -34
		check_error(NEXTE, bne)
		cmp	%l4, 99
		check_error(NEXTE, bne)
		cmp	%l1, -44
		check_error(NEXTE, bne)
		cmp	%l2, 0
		check_error(NEXTE, bne)
		cmp	%l3, -9
		check_error(NEXTE, bne)
		cmp	%l5, -25
		check_error(NEXTE, bne)

/*******************************************************************************

	UNTAKEN BRANCH WITH VARIOUS DELAY and TARGET INSTRUCTIONS

*******************************************************************************/

#define A_WORD	-131
#define AN_WORD	132
		.seg	"data"
		.align	8
aWord:		.word A_WORD
		.word AN_WORD
		.seg	"text"

	! test LOAD in delay and at target.

bru_ld_ld:
		set	aWord - 135, %l0
		mov	A_WORD - 5, %l1
		cmp	%g0, -1

		bl	1f			! UNTAKEN
		ld	[%l0 + 135], %l5

		cmp	%l5, A_WORD
		check_error(NEXTE, bne)
		cmp	%l1, A_WORD - 5
		check_error(NEXTE, bne)
		b	2f
		nop
	
	1:	ld	[%l0 + 135], %l1
		check_error(NEXTE, ba)		! shouldn't be here
		b	stop_end
		nop
	
	2:

	! test STORE in delay and at target

bru_st_st:
		set	aWord - 135, %l0
		mov	A_WORD - 5, %l1
		mov	A_WORD - 1, %l5
		cmp	%g0, -1

		bl	1f			! UNTAKEN
		st	%l5, [%l0 + 135]

		ld	[%l0 + 135], %l2
		cmp	%l2, A_WORD - 1
		check_error(NEXTE, bne)
		b	2f
		nop
	
	1:	st	%l1, [%l0 + 135]	! NOT EXECUTED
		check_error(NEXTE, ba)		! shouldn't be here
		b	stop_end
		nop
	
	2:

	! test TICC in delay with load at target

bru_ticc_ld:
		cmp	%g0, -1
		mov	88, %l0
		mov	-18, %l1

		be	1f			! UNTAKEN
		t	ST_SUPER

		mov	ERR_TEMP, %l0
		cmp	ERR_TEMP, TRAP_TAKEN	! check that trap was taken
		check_error(NEXTE, bne)
		cmp	%l0, TRAP_TAKEN		! check that instr. after ticc
		check_error(NEXTE, bne)		! was executed after the trap
		cmp	%l1, -18		! make sure target was annulled
		check_error(NEXTE, bne)
		b	2f
		nop
	
	1:	ld	[%l0 + 135], %l1
		check_error(NEXTE, ba)		! shouldn't be here
		b	stop_end
		nop
	2:

	! test ldd in delay and at target.
	! NOTE: no automatic error checking: can't do until we have a gate
	! model for the FPU

bru_ldd_ldd:
		set	aWord - 135, %l0
		cmp	%g0, -1

		bl	1f			! UNTAKEN
		LDDi(L0, 135, L2)		! ldd	[%l0 + 135], %l2

		b	2f
		nop
	
	1:	LDDi(L0, 135, L4)		! ldd	[%l0 + 135], %l4
								! NOT EXECUTED
		check_error(NEXTE, ba)		! shouldn't be here
		b	stop_end
		nop
	
	2:

	! test std in delay and at target

bru_std_std:
		set	aWord - 135, %l0
		mov	-35, %l1
		mov	27, %l2
		mov	-56, %l3

		cmp	%g0, -1

		bl	1f			! UNTAKEN
		STDi(L0, L0, 135)		! std	%l0, [%l0 + 135]

		b	2f
		nop
	
	1:	STDi(L2, L0, 135)		! std	%l2, [%l0 + 135]
								! NOT EXECUTED
		check_error(NEXTE, ba)		! shouldn't be here
		b	stop_end
		nop
	2:

	! test TADD trap in delay with load at target

bru_tadd_ld:
		mov	0x31, %l0
		mov	16, %l1
		mov	-13, %l2
		set	aWord - 135, %l4
		mov	-1432, %l5
		cmp	%g0, -1

		be	1f			! UNTAKEN
		taddcctv	%l0, %l1, %l2	! TAG Trap

		mov	ERR_TEMP, %l3
		cmp	ERR_TEMP, TRAP_TAKEN	! check that trap was taken
		check_error(NEXTE, bne)
		cmp	%l3, TRAP_TAKEN		! check that instr. after trap
		check_error(NEXTE, bne)		! was executed after the trap
		cmp	%l2, -13		! make sure trapped instr.
						! didn't write
		check_error(NEXTE, bne)
		cmp	%l5, -1432		! make sure target was annulled
		check_error(NEXTE, bne)
		b	2f
		nop
	
	1:	ld	[%l4 + 135], %l5
		check_error(NEXTE, ba)		! shouldn't be here
		b	stop_end
		nop
	2:

/********************************************************************
bru_fpop_std:
		set	aWord - 135, %l0
		cmp	%g0, -1

		bl	1f			! UNTAKEN
		faddd	%f0, %f2, %f4

		b	2f
		nop
	
	1:	std	%f2, [%l0 + 135]	! NOT EXECUTED
		check_error(NEXTE, ba)		! shouldn't be here
		b	stop_end
		nop
	2:
**********************************************************************/
	

/*******************************************************************************

	END OF TEST CASES

*******************************************************************************/

		mov	0,%g6
		b	stop_end
		nop


neverGetHere:
		mov	-1, %l4		! this instruction is annuled because
					! it is the target of untaken branches

		check_error(NEXTE, ba)	! always an error to arrive here

stop_end:
		cmp	%g0,%g6
		check_error(NEXTE, bne)
		nop

stop: b testok; nop
		b	stop		! enter an infinite loop for Daisy
		nop

		ret			! normal procedure end
		restore



/*******************************************************************************
*******************************************************************************/

