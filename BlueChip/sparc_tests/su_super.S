
! This is a startup program to make it easier to run programs in "machine mode"
! on sas.                                          

! The PSR, WIM, and TBR registers are set-up, and a stack pointer
! suitable for the daisy environment is established.


.seg "text"
.align 8
stack_bot:
	.skip 4096
stack_top:
.seg "text"
		BREAKPOINT  = 0xff
		INITIAL_PSR = 0xe0	! ET, S, pS, CWP = 0
		INITIAL_WIM = 0x2	! trap window is window 1
		INITIAL_TBR = address_0

.seg	"text"
start:
		! establish PSR
		set	INITIAL_PSR, %g1
		wr	%g1, %psr		! setup PSR

		! establish WIM
		set	INITIAL_WIM, %g1
		wr	%g1, %wim

		! establish TBR
		set	INITIAL_TBR, %g1
		wr	%g1, %tbr
		nop
		nop
		nop



		! establish SP
		set	stack_top, %sp

		sub	%sp, 64, %sp		! make room for start-up frame

		mov	0, %fp			! startup routine sets
						! its frame pointer to 0
						! so that top frame on the stack
						! is recognizeable
		call	main
		nop
		t	BREAKPOINT
