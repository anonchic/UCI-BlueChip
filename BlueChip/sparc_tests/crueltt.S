/*	@(#)crueltt.s	1.15	4/14/89
	trap table and window over/under flow trap handlers
*/


!----------------------------------------------------------------------
! Link in trap handling routines by #defining the entry point before
! #including crueltt.


#ifndef UNIMP_HANDLER
#define UNIMP_HANDLER	unimplemented_trap
#endif

#ifndef PRIV_HANDLER
#define PRIV_HANDLER	unimplemented_trap
#endif

#ifndef FPDIS_HANDLER
#define FPDIS_HANDLER	unimplemented_trap
#endif

#ifndef CPDIS_HANDLER
#define CPDIS_HANDLER	unimplemented_trap
#endif

#ifndef OFL_HANDLER
#define OFL_HANDLER	win_overflow
#endif

#ifndef UFL_HANDLER
#define UFL_HANDLER	win_underflow
#endif

#ifndef MALGN_HANDLER
#define MALGN_HANDLER	unimplemented_trap
#endif

#ifndef FPX_HANDLER
#define FPX_HANDLER	unimplemented_trap
#endif

#ifndef CPX_HANDLER
#define CPX_HANDLER	unimplemented_trap
#endif

#ifndef TAGOF_HANDLER
#define TAGOF_HANDLER	unimplemented_trap
#endif

! register file parity error handler
#ifndef RFE_HANDLER
#define RFE_HANDLER	unimplemented_trap
#endif

!----------------------------------------------------------------------

#include "exc_handlers.S"

#define TRAP(label) b label; rd %psr, %l0; nop; nop

	.balign 4096
address_0:

T_reset:			mov 0xfa0, %psr
				mov	0, %tbr
				b start
				mov	0, %wim
T_instr_access_exception:					! 1
#ifdef IAE_HANDLER
				TRAP(IAE_HANDLER)
#else
				iae_handler
#endif
T_unimplemented_instruction:	TRAP(UNIMP_HANDLER)	        ! 2
T_privileged_instruction:	TRAP(PRIV_HANDLER)	        ! 3
T_fp_disabled:			TRAP(FPDIS_HANDLER)	        ! 4
T_window_overflow:		TRAP(OFL_HANDLER)		! 5
T_window_underflow:		TRAP(UFL_HANDLER)		! 6
T_mem_addr_not_aligned:		TRAP(MALGN_HANDLER)	        ! 7
T_fp_exception:			TRAP(FPX_HANDLER)               ! 8
T_data_access_exception:					! 9
#ifdef DAE_HANDLER
				TRAP(DAE_HANDLER)
#else
				dae_handler
#endif
T_tag_overflow:			TRAP(TAGOF_HANDLER)	        ! 10

				TRAP(unimplemented_trap)        ! 11
				TRAP(unimplemented_trap)        ! 12
				TRAP(unimplemented_trap)        ! 13
				TRAP(unimplemented_trap)        ! 14
				TRAP(unimplemented_trap)        ! 15
				TRAP(unimplemented_trap)        ! 16


T_int_1:			int_handler			! 17
T_int_2:			int_handler			! 18
T_int_3:			int_handler			! 19
T_int_4:			int_handler			! 20
T_int_5:			int_handler			! 21
T_int_6:			int_handler			! 22
T_int_7:			int_handler			! 23
T_int_8:			int_handler			! 24
T_int_9:			int_handler			! 25
T_int_10:			int_handler			! 26
T_int_11:			int_handler			! 27
T_int_12:			int_handler			! 28
T_int_13:			int_handler			! 29
T_int_14:							! 30
#ifdef INT14_HANDLER
				TRAP(INT14_HANDLER)
#else
				int_handler
#endif
T_int_15:			TRAP(unimplemented_trap)	! 31
T_rferr:                        TRAP(RFE_HANDLER)               ! 32
T_iaerr:							! 33
#ifdef IAER_HANDLER
				TRAP(IAER_HANDLER)
#else
				iae_handler
#endif
                                TRAP(unimplemented_trap)
                                TRAP(unimplemented_trap)
T_cp_disabled:                  TRAP(CPDIS_HANDLER)             ! 36
                                TRAP(unimplemented_trap)
                                TRAP(unimplemented_trap)
                                TRAP(unimplemented_trap)
T_cp_exception:                 TRAP(CPX_HANDLER)               ! 40
T_daerr:                        				! 41
#ifdef DAER_HANDLER
				TRAP(DAER_HANDLER)
#else
				dae_handler
#endif
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
!----------------------------------------------------------------------
! Link in software trap routines by #defining the entry point before
! #including crueltt.

#ifndef SWTRAP_0
#define SWTRAP_0	set_super
#endif

#ifndef SWTRAP_1
#define SWTRAP_1	unimplemented_trap
#endif

#ifndef SWTRAP_2
#define SWTRAP_2	unimplemented_trap
#endif

#ifndef SW_TRAP_HANDLER
#define other_swtraps	unimplemented_trap
#endif

!----------------------------------------------------------------------

				TRAP(SWTRAP_0)
				TRAP(SWTRAP_1)
				TRAP(SWTRAP_2)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)
				TRAP(other_swtraps)

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
