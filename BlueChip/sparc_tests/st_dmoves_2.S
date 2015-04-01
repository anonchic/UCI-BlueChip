/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: dmoves_2.s,v 1.2 91/03/15 10:48:03 rudi Exp $"
.align 8
.seg "text"
/*********************************************************************************


	PURPOSE:

	This diagnostic tests pairs of instructions where the first loads
	a register (either a ld or an alu op) and the second stores it
	in memory.  Various cases and combinations are used.  Another
	case tested is when the register is not only the source to be
	stored, but is used in the address calculation.  Floating point
	loads and stores are also included (with the flag FPU_EN to include
	or exclude the test cases).

	This diagnostic was modified to conform to the error reporting
	standard (ATL, August 1987).  Full documentation for this standard
	can be found in include/err_rprt.doc

	All errors are logged in memory starting at log_area.  Ignoring the
	first half word, the half word numbers identify the test that failed.
	Find the call to:      
		check_error(NUM, ...)

	in this source.  This is the point at which the error was detected.


*********************************************************************************/


/* macro for checking errors */

#define	ERR_RET	%r12		/* make %o4 the error reporting reg */
#define ERR_RET_NUM 12

#include "err_macro.h"

/********************************************************************************

      TRAP TABLE

********************************************************************************/

#include "err_incr.h"

#include "assy_macros.h"

new_init(dmoves_2)
	
#include "crueltt.S"


/********************************************************************************

      STARTUP ROUTINE

********************************************************************************/

start:
		set	0x10e0, %g1		! EF, S, PS, ET
		mov     %g1, %psr
		mov     0, %wim
		set	address_0, %g1
		mov     %g1, %tbr; nop; nop; nop
		clr	%g2
		clr	%g3

		clr	%l0
		clr	%l1
		clr	%l2
		clr	%l3
		clr	%l4
		clr	%l5
		clr	%l6	
		clr	%l7
	
/*******************************************************************************

      MAIN PROGRAM

*******************************************************************************/


		set	3f, %l0			! point to data
ld_st:
		ld	[%l0], %l1
		st	%l1, [%l0 + 4]

		ld	[%l0 + 4], %l2
		add	%l1, %l2, %l3
		cmp	%l3, 90
		check_error(NEXTE, bne)

//good
	
ld_std_even:
		mov	-3, %l3
		ld	[%l0], %l2
	nop	            ! Added by murph. is this really required? Maybe a trap should fix it.
		std	%l2, [%l0 + 8]
		ldd	[%l0 + 8], %l4

	
		add	%l4, %l5, %l6
		cmp	%l6, 42
		check_error(NEXTE, bne)

//bad

ld_std_odd:
		mov	-3, %l2
		ld	[%l0], %l3
		std	%l2, [%l0 + 8]

		ldd	[%l0 + 8], %l4
		add	%l4, %l5, %l6
		cmp	%l6, 42
		check_error(NEXTE, bne)
	

		.seg	"data"
		.align	8
	3:	.word	45, 0, 0, 0, 0, 0
		.seg	"text"
	
!------------------------------------------------------------------------------

		set	3f, %l0
ldd_st_even:
		ldd	[%l0], %l2
		st	%l2, [%l0 + 8]

		ld	[%l0 + 8], %l5
		add	%l2, %l3, %l4
		add	%l5, %l3, %l6
		cmp	%l4, %l6
		check_error(NEXTE, bne)

ldd_st_odd:
		ldd	[%l0], %l2
		st	%l3, [%l0 + 12]

		ld	[%l0 + 12], %l5
		add	%l2, %l3, %l4
		add	%l2, %l5, %l6
		cmp	%l4, %l6
		check_error(NEXTE, bne)

ldd_std:
		ldd	[%l0], %l2
		std	%l2, [%l0 + 16]

		ldd	[%l0 + 16], %l4
		add	%l2, %l3, %l1
		add	%l4, %l5, %l6
		cmp	%l1, %l6
		check_error(NEXTE, bne)

		.seg	"data"
		.align	8
	3:	.word	27, 21, -1, -1, -1, -1
		.seg	"text"


!-------------------------------------------------------------------------------

		set	3f, %l0
alu_src:
		set	-25, %l1
		add	%l1, 61, %l2
		st	%l2, [%l0]

		ld	[%l0], %l3
		cmp	%l3, 36
		check_error(NEXTE, bne)


alu_addr_src_st:
		sub	%l0, 27, %l7
	
		mov	-7, %l6
		add	%l6, 42, %l5
		st	%l5, [%l7 + %l5]	! loc= %l0 + 8

		ld	[%l0 + 8], %l4
		cmp	%l4, 35
		check_error(NEXTE, bne)


		.seg	"data"
		.align	8
	3:	.word	-1, -1, -1
		.seg	"text"

!------------------------------------------------------------------------------

		set	3f, %l0
alu_std_even:
		set	-20, %l3
		add	%l0, %l3, %l2
		std	%l2, [%l0]

		ldd	[%l0], %l4
		add	%l2, %l3, %l6
		add	%l4, %l5, %l7
		cmp	%l6, %l7
		check_error(NEXTE, bne)

		add	%l4, 20, %l1
		cmp	%l1, %l0
		check_error(NEXTE, bne)

alu_std_odd:
		set	-30, %l2
		add	%l0, %l2, %l3
		std	%l2, [%l0 + 8]

		ldd	[%l0 + 8], %l4
		add	%l2, %l3, %l6
		add	%l4, %l5, %l7
		cmp	%l6, %l7
		check_error(NEXTE, bne)

		add	%l5, 30, %l1
		cmp	%l1, %l0
		check_error(NEXTE, bne)

alu_addr_src_std:
		sub	%l0, 10, %l7
		mov	3, %l2
		add	%l2, 23, %l3
		std	%l2, [%l7 + %l3]	! loc= %l0 + 16

		ldd	[%l0 + 16], %l4
		cmp	%l2, %l4
		check_error(NEXTE, bne)

		cmp	%l3, %l5
		check_error(NEXTE, bne)


		.seg	"data"
		.align	8
	3:	.word	-1, -1, -1, -1, -1, -1
		.seg	"text"

!-------------------------------------------------------------------------------

		set	3f, %l0
swap_st:
		mov	45, %l1
		swap	[%l0 + %g0], %l1
		st	%l1, [%l0 + %l1]

		ld	[%l0 + %g0], %l2
		ld	[%l0 + 8], %l3

		cmp	%l2, 45
		check_error(NEXTE, bne)

		cmp	%l3, 8
		check_error(NEXTE, bne)

ld_swap:
		ld	[%l0 + 12], %l1
		swap	[%l0 + %l1], %l1

		ld	[%l0 + 16], %l2
		cmp	%l2, 16
		check_error(NEXTE, bne)

		cmp	%l1, 90
		check_error(NEXTE, bne)


		.seg	"data"
		.align	8
	3:	.word	8, -1, -1, 16, 90
		.seg	"text"

!-------------------------------------------------------------------------------

/*		set	3f, %l0
swapa_st:
		mov	45, %l1
		swapa	[%l0 + %g0]0xa, %l1
		st	%l1, [%l0 + %l1]

		ld	[%l0 + %g0], %l2
		ld	[%l0 + 8], %l3

		cmp	%l2, 45
		check_error(NEXTE, bne)

		cmp	%l3, 8
		check_error(NEXTE, bne)

ld_swapa:
		ld	[%l0 + 12], %l1
		swapa	[%l0 + %l1]0xa, %l1

		ld	[%l0 + 16], %l2
		cmp	%l2, 16
		check_error(NEXTE, bne)

		cmp	%l1, 90
		check_error(NEXTE, bne)

		.seg	"data"
		.align	8
	3:	.word	8, -1, -1, 16, 90
		.seg	"text" */

!------------------------------------------------------------------------------

		set	3f, %l0
ldst_st:
		ldstub	[%l0 + 3], %l1
		st	%l1, [%l0 + %l1]

		ldsh	[%l0 + 2], %l3
		ld	[%l0 + %l1], %l4

		set	0xff, %l5
		cmp	%l3, %l5
		check_error(NEXTE, bne)

		cmp	%l4, 4
		check_error(NEXTE, bne)

alu_ldst:
		set	-10, %l1
		add	%l1, 18, %l2
		ldstub	[%l0 + %l2], %l5

		ld	[%l0 + 8], %l3
		set	0xff000001, %l4
		cmp	%l3, %l4
		check_error(NEXTE, bne)

		cmp	%l5, 0x1a
		check_error(NEXTE, bne)


		.seg	"data"
		.align	8
	3:	.word	4, -1, 0x1a000001
		.seg	"text"

!-------------------------------------------------------------------------------

#ifdef FPU_EN
		set	3f, %l0			! point to data
f_ld_st:
		ld	[%l0], %f1
		st	%f1, [%l0 + 4]

		ld	[%l0 + 4], %l2
		cmp	%l2, 45
		check_error(NEXTE, bne)

f_ld_std_even:
		ld	[%l0 + 24], %f3
		ld	[%l0], %f2
		std	%f2, [%l0 + 8]

		ldd	[%l0 + 8], %l4
		add	%l4, %l5, %l6
		cmp	%l6, 42
		check_error(NEXTE, bne)

f_ld_std_odd:
		ld	[%l0 + 24], %f2
		ld	[%l0], %f3
		std	%f2, [%l0 + 8]

		ldd	[%l0 + 8], %l4
		add	%l4, %l5, %l6
		cmp	%l6, 42
		check_error(NEXTE, bne)


		.seg	"data"
		.align	8
	3:	.word	45, 0, 0, 0, 0, 0, -3
		.seg	"text"

#endif FPU_EN

!------------------------------------------------------------------------------

#ifdef FPU_EN
		set	3f, %l0
f_ldd_st_even:
		ldd	[%l0], %f2
		st	%f2, [%l0 + 8]

		st	%f3, [%l0 + 12]
		ldd	[%l0 + 8], %l4
		add	%l4, %l5, %l6
		cmp	%l6, 48
		check_error(NEXTE, bne)

f_ldd_st_odd:
		ldd	[%l0 + 32], %f4
		st	%f5, [%l0 + 16]

		st	%f4, [%l0 + 20]
		ldd	[%l0 + 16], %l4
		add	%l4, %l5, %l6
		cmp	%l6, 36
		check_error(NEXTE, bne)

f_ldd_std:
		ldd	[%l0], %f8
		std	%f8, [%l0 + 24]

		ldd	[%l0 + 24], %l4
		add	%l4, %l5, %l6
		cmp	%l6, 48
		check_error(NEXTE, bne)

		.seg	"data"
		.align	8
	3:	.word	27, 21, -1, -1, -1, -1, -1, -1, 30, 6
		.seg	"text"

#endif FPU_EN

/*******************************************************************************

	ALL DONE

*******************************************************************************/

stop: b testok; nop
		b	stop


/*******************************************************************************

      SUPPORT ROUTINES

*******************************************************************************/

#include "err_subr.S"


/*******************************************************************************
*******************************************************************************/

