.text

testok:
	set	__saved_ret, %o1
	st	%g0, [%o1]
	b	st_exit
	nop
fail:
	set	__saved_ret, %o1
	set	1, %g1
	st	%g1, [%o1]	
	b	st_exit
	nop

testretg1:
	set	__saved_ret, %o1
	st	%g1, [%o1]

	b	st_exit
	nop	

savestate:
	! save global and input registers
	
	nop; nop; nop

	set	__saved_g1, %o1
	st	%g1, [%o1]
	set	__saved_g2, %o1	
	st	%g2, [%o1]
	set	__saved_g3, %o1	
	st	%g3, [%o1]
	set	__saved_g4, %o1	
	st	%g4, [%o1]
	set	__saved_g5, %o1	
	st	%g5, [%o1]
	set	__saved_g6, %o1	
	st	%g6, [%o1]
	set	__saved_g7, %o1
	st	%g7, [%o1]

	set	__saved_l0, %o1
	st	%l0, [%o1]
	set	__saved_l1, %o1
	st	%l1, [%o1]
	set	__saved_l2, %o1
	st	%l2, [%o1]
	set	__saved_l3, %o1
	st	%l3, [%o1]
	set	__saved_l4, %o1
	st	%l4, [%o1]
	set	__saved_l5, %o1
	st	%l5, [%o1]
	set	__saved_l6, %o1
	st	%l6, [%o1]
	set	__saved_l7, %o1
	st	%l7, [%o1]
	
	set	__saved_i0, %o1
	st	%i0, [%o1]
	set	__saved_i1, %o1
	st	%i1, [%o1]
	set	__saved_i2, %o1
	st	%i2, [%o1]
	set	__saved_i3, %o1
	st	%i3, [%o1]
	set	__saved_i4, %o1
	st	%i4, [%o1]
	set	__saved_i5, %o1
	st	%i5, [%o1]
	set	__saved_i6, %o1
	st	%i6, [%o1]
	set	__saved_i7, %o1
	st	%i7, [%o1]

	set	__saved_o2, %o1
	st	%o2, [%o1]
	set	__saved_o3, %o1
	st	%o3, [%o1]
	set	__saved_o4, %o1
	st	%o4, [%o1]
	set	__saved_o5, %o1
	st	%o5, [%o1]
	set	__saved_o6, %o1
	st	%o6, [%o1]
	set	__saved_o7, %o1
	st	%o7, [%o1]
	
	set	__saved_psr, %o1
	mov 	%psr, %l1
	st	%l1, [%o1]

	set	__saved_wim, %o1
	mov 	%wim, %l1
	st	%l1, [%o1]

	set	__saved_tbr, %o1
	mov 	%tbr, %l1
	st	%l1, [%o1]

	! I wonder if anything cares..
	mov	%g0, %g1
	mov	%g0, %g2
	mov	%g0, %g3
	mov	%g0, %g4
	mov	%g0, %g5
	mov	%g0, %g6
	mov	%g0, %g7

	mov	%g0, %l0
	mov	%g0, %l1	
	mov	%g0, %l2
	mov	%g0, %l3
	mov	%g0, %l4
	mov	%g0, %l5
	mov	%g0, %l6
	mov	%g0, %l7

	mov	%g0, %i0
	mov	%g0, %i1	
	mov	%g0, %i2
	mov	%g0, %i3
	mov	%g0, %i4
	mov	%g0, %i5
	mov	%g0, %i6
	mov	%g0, %i7

/*	set	0xe0, %g1
	mov	%g1, %psr
	nop
	nop
	nop

	set 	0x2, %g1
	mov	%g1, %wim
	nop
	nop
	nop
	

	set 	0x0, %g1
	mov	%g1, %tbr
	nop
	nop
	nop  */

	b start
	nop

	.data
__saved_psr:	
	.word 0
__saved_wim:
	.word 0
__saved_tbr:
	.word 0
__saved_g1:
	.word 0	
__saved_g2:
	.word 0	
__saved_g3:
	.word 0	
__saved_g4:
	.word 0	
__saved_g5:
	.word 0	
__saved_g6:
	.word 0	
__saved_g7:
	.word 0
__saved_l0:
 	.word 0	
__saved_l1:
	.word 0	
__saved_l2:
	.word 0	
__saved_l3:
	.word 0	
__saved_l4:
	.word 0	
__saved_l5:
	.word 0	
__saved_l6:
	.word 0	
__saved_l7:
	.word 0
__saved_i0:
	.word 0	
__saved_i1:
	.word 0	
__saved_i2:
	.word 0	
__saved_i3:
	.word 0	
__saved_i4:
	.word 0	
__saved_i5:
	.word 0	
__saved_i6:
	.word 0	
__saved_i7:
	.word 0
__saved_o2:
	.word 0	
__saved_o3:
	.word 0	
__saved_o4:
	.word 0	
__saved_o5:
	.word 0	
__saved_o6:
	.word 0	
__saved_o7:
	.word 0
__saved_ret:
	.word 0		
	
	.text

st_exit:

	! I must be sure that the current and saved windows are valid, so..
	mov	0, %wim
	nop;nop;nop;

	set	__saved_psr, %o1
	ld	[%o1], %l1
	mov 	%l1, %psr
	nop;nop;nop;

	set	__saved_wim, %o1
	ld	[%o1], %l1
	mov 	%l1, %wim
	nop;nop;nop;

	set	__saved_tbr, %o1
	ld	[%o1], %l1
	mov 	%l1, %tbr
	nop;nop;nop;

	set	__saved_g1, %o1
	ld	[%o1], %g1
	set	__saved_g2, %o1	
	ld	[%o1], %g2
	set	__saved_g3, %o1	
	ld	[%o1], %g3
	set	__saved_g4, %o1	
	ld	[%o1], %g4
	set	__saved_g5, %o1	
	ld	[%o1], %g5
	set	__saved_g6, %o1	
	ld	[%o1], %g6
	set	__saved_g7, %o1
	ld	[%o1], %g7

	set	__saved_l0, %o1
	ld	[%o1], %l0
	set	__saved_l1, %o1
	ld	[%o1], %l1
	set	__saved_l2, %o1
	ld	[%o1], %l2
	set	__saved_l3, %o1
	ld	[%o1], %l3
	set	__saved_l4, %o1
	ld	[%o1], %l4
	set	__saved_l5, %o1
	ld	[%o1], %l5
	set	__saved_l6, %o1
	ld	[%o1], %l6
	set	__saved_l7, %o1
	ld	[%o1], %l7
	
	set	__saved_i0, %o1
	ld	[%o1], %i0
	set	__saved_i1, %o1
	ld	[%o1], %i1
	set	__saved_i2, %o1
	ld	[%o1], %i2
	set	__saved_i3, %o1
	ld	[%o1], %i3
	set	__saved_i4, %o1
	ld	[%o1], %i4
	set	__saved_i5, %o1
	ld	[%o1], %i5
	set	__saved_i6, %o1
	ld	[%o1], %i6
	set	__saved_i7, %o1
	ld	[%o1], %i7

	set	__saved_o2, %o1
	ld	[%o1], %o2
	set	__saved_o3, %o1
	ld	[%o1], %o3
	set	__saved_o4, %o1
	ld	[%o1], %o4
	set	__saved_o5, %o1
	ld	[%o1], %o5
	set	__saved_o6, %o1
	ld	[%o1], %o6
	set	__saved_o7, %o1
	ld	[%o1], %o7

	
	nop; nop; nop	

last:

	set	__saved_ret, %o1
	ld	[%o1], %o0	

	set 0xffff8, %o2
	st %o0, [%o2]

!	mov	%o0, %i0

	wr %g0, %asr0
	retl
	nop
!	restore


