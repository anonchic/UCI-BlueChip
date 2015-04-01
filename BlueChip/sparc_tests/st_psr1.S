/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: psr1.s,v 1.1 91/01/10 12:47:48 rudi Exp $"
.balign 8
.seg "text"
! this programs tests all the instructions that change cwp.
! wrPSR, save, restore, rett,
!%r7 is used to log errors a non zero in r7 indicates an error

 
/* macro for checking errors */
 
#define ERR_RET %r14              /* make %o6 the error return reg */
#define ERR_RET_NUM 14 
#include "err_incr.h"
#include "err_macro.h"

new_init(psr1)
 


/******************************************************************************
  TRAP TABLE
	@(#)crueltt.s	1.7	11/6/87
	trap table and window over/under flow trap handlers
	for use on Verilog description of custom sunrise.
******************************************************************************/

#include "exc_handlers.S"

#define TRAP(label) b label; rd %psr, %l0; nop; nop

.balign 4096
address_0:
T_reset:			mov 0xfa0, %psr
	                        set address_0, %g1
				mov	%g1, %tbr
				b start
				mov	0, %wim
T_instr_access_exception:	iae_handler			! 1
T_unimplemented_instruction:	TRAP(unimplemented_trap)	! 2
T_privileged_instruction:	TRAP(unimplemented_trap)	! 3
T_fp_disabled:			TRAP(unimplemented_trap)	! 4
T_window_overflow:		TRAP(win_overflow)		! 5
T_window_underflow:		TRAP(win_underflow)		! 6
T_mem_addr_not_aligned:		TRAP(unimplemented_trap)	! 7
T_fp_exception:			TRAP(unimplemented_trap)        ! 8
T_data_access_exception:	dae_handler			! 9
T_tag_overflow:			TRAP(unimplemented_trap)	! 10

				TRAP(unimplemented_trap)        ! 11
				TRAP(unimplemented_trap)        ! 12
				TRAP(unimplemented_trap)        ! 13
				TRAP(unimplemented_trap)        ! 14
				TRAP(unimplemented_trap)        ! 15
				TRAP(unimplemented_trap)        ! 16


T_int_1:			TRAP(unimplemented_trap)	! 17
T_int_2:			TRAP(unimplemented_trap)	! 18
T_int_3:			TRAP(unimplemented_trap)	! 19
T_int_4:			TRAP(unimplemented_trap)	! 20
T_int_5:			TRAP(unimplemented_trap)	! 21
T_int_6:			TRAP(unimplemented_trap)	! 22
T_int_7:			TRAP(unimplemented_trap)	! 23
T_int_8:			TRAP(unimplemented_trap)	! 24
T_int_9:			TRAP(unimplemented_trap)	! 25
T_int_10:			TRAP(unimplemented_trap)	! 26
T_int_11:			TRAP(unimplemented_trap)	! 27
T_int_12:			TRAP(unimplemented_trap)	! 28
T_int_13:			TRAP(unimplemented_trap)	! 29
T_int_14:			int_handler			! 30
T_int_15:			TRAP(unimplemented_trap)	! 31
T_rferr:			TRAP(unimplemented_trap)	! 32
T_iaerr:			iae_handler			! 33
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
T_cp_disabled:			TRAP(unimplemented_trap)	! 36
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
T_cp_exception:			TRAP(unimplemented_trap)	! 40
T_daer:				dae_handler			! 41
				
				! unused hardware trap vectors    42 - 127

				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)

software_traps:							! 128 - 255

				TRAP(set_super)
				TRAP(trap_handler)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)
				TRAP(unimplemented_trap)

unimplemented_trap:
		b	unimplemented_trap;
		nop

lastWindowMask:	.word 0

win_overflow:
	! a simple window overflow handler.
	! This handler does not check for page faults,
	! It assumes memory on the stack is resident.
	! This code cannot be interrupted.

	! This code not recommended for use on real hardware

		rd	%wim, %l3	! save wim before clearing it
		mov	0, %wim
		nop
		nop
		nop

computeLastWindowMask:
		set	lastWindowMask, %l5	! may have mask in memory
		ld	[%l5], %l4
		cmp	%l4, 0
		bne	haveLastWindowMask
		nop

		! must figure out how many windows there are.
		! This only has to be done once.

		mov	%g1, %l4		! free up three globals
		mov	%g2, %l5
		mov	%g3, %l6
		mov	%l0, %g1		! save psr in g1 for access
						! from another window
		and	%g1, 0x1f, %l7		! get CWP
		mov	1, %g2
		sll	%g2, %l7, %g2		! window mask for trap window

1:		restore				! increment CWP until
		rd	%psr, %g3		! it wraps around to zero.
		and	%g3, 0x1f, %g3		! extract CWP
		cmp	%g3, 0
		be	1f
		nop
		b	1b
		sll	%g2, 1, %g2

1:		wr	%g1, %psr		! restore trap window
		nop
		nop
		nop
		mov	%l4, %g1		! restore g1
		mov	%g2, %l4		! lastWindowMask
		mov	%l5, %g2		! restore g2 and g3
		mov	%l6, %g3
		set	lastWindowMask, %l5
		st	%l4, [%l5]		! save mask in memory

haveLastWindowMask:

		save			! decrement CWP to enter window to save
		
		st	%l0, [%sp + 0]	! save locals onto the stack
		st	%l1, [%sp + 4]
		st	%l2, [%sp + 8]
		st	%l3, [%sp + 12]
		st	%l4, [%sp + 16]
		st	%l5, [%sp + 20]
		st	%l6, [%sp + 24]
		st	%l7, [%sp + 28]

		st	%i0, [%sp + 32]	! save in's onto the stack
		st	%i1, [%sp + 36]
		st	%i2, [%sp + 40]
		st	%i3, [%sp + 44]
		st	%i4, [%sp + 48]
		st	%i5, [%sp + 52]
		st	%i6, [%sp + 56]	! frame pointer
		st	%i7, [%sp + 60]	! return address

		restore			! return to trap window

		cmp	%l3, 1		! %l3 contains the saved WIM
		be	rotate_right	! do we have to rotate around the end?
		srl	%l3, 1, %l3	! SHIFT WIM mask

restoreWIM:
		wr	%l3, %wim	! put in new wim
		nop			! need 3 instruction delay
		jmp	%l1		! and return from trap
		rett	%l2

rotate_right:
		mov	%l4, %wim	! put in new wim. from lastWindowMask
		jmp	%l1		! and return from trap
		rett	%l2



win_underflow:
	
		rd	%wim, %l3	! save WIM before clearing it
		mov	0, %wim
		nop
		nop
		nop

		set	lastWindowMask, %l5	! get mask from memory
		ld	[%l5], %l4

		restore			! move to window which needs restoring
		restore

		ld	[%sp + 0], %l0	! restore locals from stack
		ld	[%sp + 4], %l1
		ld	[%sp + 8], %l2
		ld	[%sp + 12], %l3
		ld	[%sp + 16], %l4
		ld	[%sp + 20], %l5
		ld	[%sp + 24], %l6
		ld	[%sp + 28], %l7

		ld	[%sp + 32], %i0	! restore in's from the stack
		ld	[%sp + 36], %i1
		ld	[%sp + 40], %i2
		ld	[%sp + 44], %i3
		ld	[%sp + 48], %i4
		ld	[%sp + 52], %i5
		ld	[%sp + 56], %i6
		ld	[%sp + 60], %i7

		save			! return to trap window
		save

		cmp	%l3, %l4	! %l3 contains saved WIM
		be	rotate_left	! do we have to rotate around the end?
		sll	%l3, 1, %l3	! SHIFT WIM mask

restoreWIMu:
		mov	%l3, %wim	! put in new wim
		jmp	%l1		! and return from trap
		rett	%l2

rotate_left:
		mov	1, %wim		! put in new wim
		jmp	%l1		! and return from trap
		rett	%l2

set_super:
		set	0xc0, %l7	! sets S and PS
		mov	%psr, %l6
		or	%l7, %l6, %l7
		mov	%l7, %psr
		nop ; nop ; nop ; nop

		jmp	%l2
		rett	%l2 + 4



/************************************************************************
 
	ERROR REPORTING SUBROUTINE
 
************************************************************************/
 
 
#include "err_subr.S"
 
/*************************************************************************/

/*  #include "regdefs.h"    included explicitly for 4.0          */

#undef hi
#undef lo 
 





trap_handler:	mov	%psr,%r19			!s=1,ps=1,et=0,cwp=2
		nop
		and	%r19,0xfff,%r19		!mask out unwanted bits
		cmp	%r19, 0xc2
		check_error(NEXTE, bne)

r17test:	set	test_trap+4, %r19
		subcc	%r17,%r19,%r0
		check_error(NEXTE, bne)

r18test:	set	test_trap+8, %r20
		subcc	%r18,%r20,%r0
		check_error(NEXTE, bne)
		
return:		jmp	%r18+4			!this trap handler returns
		rett	%r18+8			!to trpped inst+8
		nop

/******************************************************************************
	Put before start wince it set ET=0
******************************************************************************/

rett:		mov 	0xc4,%psr		!s=1, ps=1, et=0, cwp=4
		nop				
		nop
		nop
		jmp	%g1			! test_rett
		rett	%g1 + 4			! test_rett+4
		nop
		nop
		nop

/******************************************************************************

	STARTUP

******************************************************************************/

start:
		mov	0xa4,%psr		!cwp=4, s=1, et=1
		mov	0x01,%wim
		set 	address_0, %g1
		mov	%g1,%tbr

! generate constants

		sethi	%hi(0x11111111),%r1
		or	%r1,%lo(0x11111111),%r1
		sethi	%hi(0x22222222),%r2
		or	%r2,%lo(0x22222222),%r2
		sethi	%hi(0x33333333),%r3
		or	%r3,%lo(0x33333333),%r3
		sethi	%hi(0x44444444),%r4
		or	%r4,%lo(0x44444444),%r4
		sethi	%hi(0x55555555),%r5
		or	%r5,%lo(0x55555555),%r5

		or	%r0,0x1,%r6		!error pointer
		or	%r0,%r0,%r7


!test for wrpsr 
test_psr:	mov	0xa5,%psr
		nop
		nop
		nop
		mov	%psr,%r24
		nop
		and	%r24,0xfbf,%r24		!mask out unwanted bits(PS too)
		cmp	%r24, 0xa5
		check_error(NEXTE, bne)

!initialize registers in windows 2,3,4,5
init:		mov	%r1,%r8			!o0(w5) = 11111111
		mov	%r2,%r9			!o1(w5) = 22222222
		mov	%r3,%r10		!o2(w5) = 33333333

		mov	0xa4,%psr
		nop
		nop
		nop
		mov	%r1,%r9			!o1(w4) = 11111111
		mov	%r2,%r10		!o2(w4) = 22222222
		mov	%r3,%r11		!03(w4) = 33333333

		mov	0xa3,%psr
		nop
		nop
		nop
		mov	%r1,%r10		!o2(w3) = 11111111
		mov	%r2,%r11		!o3(w3) = 22222222
		mov	%r3,%r12		!04(w3) = 33333333

		mov	0xa2,%psr
		nop
		nop
		nop
		mov	%r1,%r11		!o3(w2) = 11111111
		mov	%r2,%r12		!o4(w2) = 22222222
		mov	%r3,%r13		!o5(w2) = 33333333

save:		mov	0xa3,%psr		!w=3
		nop
		nop
		nop
		save	%r10,%r11,%r16		!l0(w2) = 33333333
		subcc	%r3,%r16,%r0
		check_error(NEXTE, bne)

restore:	restore %r12,%r13,%r16		!l0(w3)=55555555
		subcc	%r5,%r16,%r0
		check_error(NEXTE, bne)

test_trap:	sll	%r6,0x1,%r6
		ta	0x81			!trap to loc. 129
		or	%r6,%r7,%r7		!no
		set	test_rett, %g1		! use %g1 to get back with
		ba	rett
		nop


test_rett:
		set	0x11111111, %g1		! restore %g1 to orig value
		add	%r8,%r9,%r11		!test to make sure cwp pipe
		add	%r9,%r10,%r12		!works
		subcc	%r11,%r3,%r0
		check_error(NEXTE, bne)

test_rett1:	subcc	%r12,%r5,%r0
		check_error(NEXTE, bne)
		
test_rett2:	mov	%psr, %r17		!l1(w5) = 000000e5	
		nop
		and	%r17,0xfff,%r17		!mask out unwanted bits
		subcc	%r17,0xe5,%r0
		check_error(NEXTE, bne)

!error = %r7(4)
!this segment of code test for correctness of CWP under various combinations
!of save,restore,rett isnts.

!test call-save restore-jump sequence.
test_call:	call	call1			!w=5
		save	%r8,%r10,%r16		!l0(w4) = 44444444
		mov	%psr,%r18		!l2(w5) = 8c3
		nop
		and	%r18,0xfff,%r18		!mask out unwanted bits
		subcc	%r18,0xe5,%r0
		check_error(NEXTE, bne)
		bne	sav_sav
		nop
		check_error(NEXTE, bne)
		ba	sav_sav
		nop

call1:		subcc	%r16,%r4,%r0		!w=4
		check_error(NEXTE, bne)
call2:		mov	%psr,%r17
		nop
		and	%r17,0xfff,%r17		!mask out unwanted bits
		subcc	%r17,0xe4,%r0
		check_error(NEXTE, bne)
call3:		restore %r16,%r9,%r17		!l1(w5) = 55555555
		jmp	%r15+8
		nop
!error = %r7(5)
	
!execute save sequence		
sav_sav:	mov	0xa5,%psr		!cwp = 5
		nop
		nop
		nop
		save	%r8,%r9,%r18		!l2(w4) = 33333333
		save	%r9,%r11,%r18		!l2(w3) = 44444444
		save	%r11,%r12,%r18		!l2(w2) = 55555555

!check whether the above save sequence worked
		sll	%r6,0x1,%r6
		subcc	%r5,%r18,%r0
		check_error(NEXTE, bne)
		restore				!w=3
ss1:		subcc	%r4,%r18,%r0
		check_error(NEXTE, bne)
		restore				!w=4
ss2:		subcc	%r3,%r18,%r0
		check_error(NEXTE, bne)

!error = %r7(6)
rest_rest:	save	%r0,%r0,%r0		!w=3
		save	%r0,%r0,%r0		!w=2
		
		restore	%r11,%r12,%r19		!l3(w3) = 33333333
		restore	%r10,%r12,%r19		!l3(w4) = 44444444
		restore %r10,%r11,%r19		!l3(w5) = 55555555
!check whether the above save sequence worked
		sll	%r6,0x1,%r6
		subcc	%r5,%r19,%r0
		check_error(NEXTE, bne)
		save	%r0,%r0,%r0		!w=4
rr1:		subcc	%r4,%r19,%r0
		check_error(NEXTE, bne)
		save	%r0,%r0,%r0		!w3
rr2:		subcc	%r3,%r19,%r0
!error = %r7(7)
stop: b testok; nop
		nop
