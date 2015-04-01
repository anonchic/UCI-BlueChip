/******************************************************************************

		TRAP TABLE for testing combinations of instructions.
		This is #included in diagnostics for combinations.

*******************************************************************************/

/* software traps which return in user and supervisor mode respectively. */

#define ST_USER		128
#define ST_SUPER	129
#define TRAP_TAKEN	-45	/* ERR_TEMP is set to this if a
					software trap is seen */

#include "exc_handlers.S"

#define TRAP(label) b label; rd %psr, %l0; nop; nop

address_0:
	.balign 4096

T_reset:			mov	0xfa0, %psr
				mov	0, %tbr
				b start
				mov	0, %wim
T_instr_access_exception:	iae_handler			! 1
T_unimplemented_instruction:	TRAP(illegal)			! 2
T_privileged_instruction:	TRAP(privileged)		! 3
T_fp_disabled:			TRAP(fp_disabled)		! 4
T_window_overflow:		TRAP(win_overflow)		! 5
T_window_underflow:		TRAP(win_underflow)		! 6
T_mem_addr_not_aligned:		TRAP(mem_addr_misaligned)	! 7
T_fp_exception:			TRAP(fp_exception)	        ! 8
T_data_access_exception:	dae_handler			! 9
T_tag_overflow:			TRAP(tag_overflow)		! 10

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
T_rferr:                        TRAP(unimplemented_trap)        ! 32
T_iaerr:                        iae_handler                     ! 33
                                TRAP(unimplemented_trap)
                                TRAP(unimplemented_trap)
T_cp_disabled:                  TRAP(unimplemented_trap)        ! 36
                                TRAP(unimplemented_trap)
                                TRAP(unimplemented_trap)
                                TRAP(unimplemented_trap)
T_cp_exception:                 TRAP(unimplemented_trap)        ! 40
T_daerr:                        dae_handler                     ! 41
				
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

				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)
				TRAP(trap_instr)



/******************************************************************************

	TRAP HANDLERS for combination diagnostics
				traps are generally errors in these diagnostics,
				so most of the following trap handlers
				simply report an error and branch to the end.
				HOWEVER, two particular TICC traps are allowed
				in order to set or clear the S bit, and the
				tag_overflow trap is not an error. In these
				cases, the register ERR_TEMP is set to the
				value TRAP_TAKEN.

*******************************************************************************/

unimplemented_trap:
		check_error(NEXTE, ba)
		b	stop_end
		nop

instr_access:
		check_error(NEXTE, b)
		b	stop_end
		nop
illegal:
		check_error(NEXTE, b)
		b	stop_end
		nop
privileged:
		check_error(NEXTE, b)
		b	stop_end
		nop
fp_disabled:
		check_error(NEXTE, b)
		b	stop_end
		nop
mem_addr_misaligned:
		check_error(NEXTE, b)
		b	stop_end
		nop
fp_exception:
		check_error(NEXTE, b)
		b	stop_end
		nop
data_access_exc:
		check_error(NEXTE, b)
		b	stop_end
		nop
tag_overflow:
		mov	TRAP_TAKEN, ERR_TEMP
		jmp	%l2
		rett	%l2 + 4

win_overflow:
		check_error(NEXTE, b)
		b	stop_end
		nop

win_underflow:
		check_error(NEXTE, b)
		b	stop_end
		nop
	
trap_instr:
		rd	%tbr, %l3
		srl	%l3, 4, %l3
		and	%l3, 0xff, %l3
		cmp	%l3, ST_USER	! return in user mode
		be	ok_ret_to_user
		nop
		cmp	%l3, ST_SUPER	! return in supervisor mode
		be	ok_ret_to_super
		nop
		check_error(NEXTE, b)
		b	stop_end
		nop

ok_ret_to_user:
		mov	TRAP_TAKEN, ERR_TEMP
		! clear the pS bit and restore PSR
		andn	%l0, 0x40, %l0
		wr	%l0, %psr
		nop
		jmp	%l2
		rett	%l2 + 4

ok_ret_to_super:
		mov	TRAP_TAKEN, ERR_TEMP
		! set the pS bit and restore PSR
		or	%l0, 0x40, %l0
		wr	%l0, %psr
		nop
		jmp	%l2
		rett	%l2 + 4
