/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: arithmetic2.s,v 1.1 91/05/28 16:57:45 rudi Exp $"
.align 8
.seg "text"

#define ERR_RET %l0
#define ERR_RET_NUM 16

#include "err_incr.h"

#include "err_macro.h"
new_init(arithmetic2)
#include "err_subr.S"



.seg	"text"
start:

	set	0x10e0, %g1     ! EF, S, PS, ET
	mov     %g1, %psr
	mov     0, %wim
	mov     0, %tbr; nop; nop; nop

	set	0x00f00000,%o7
	
! umul
	set	umul_dat1,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	mov	%psr,%g4
	umul	%g1,%g2,%g3
	ld	[%l1+8],%o1
	mov	%psr,%o2
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	cmp	%g4,%o2		! %psr should be unchanged
	check_error(NEXTE,bne)
! umul
	set	umul_dat2,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	mov	%psr,%g4
	umul	%g1,%g2,%g3
	ld	[%l1+8],%o1
	mov	%psr,%o2
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	cmp	%g4,%o2		! %psr should be unchanged
	check_error(NEXTE,bne)
! smul
	set	smul_dat1,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	mov	%psr,%g4
	smul	%g1,%g2,%g3
	ld	[%l1+8],%o1
	mov	%psr,%o2
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	cmp	%g4,%o2		! %psr should be unchanged
	check_error(NEXTE,bne)
! smul
	set	smul_dat2,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	mov	%psr,%g4
	smul	%g1,%g2,%g3
	ld	[%l1+8],%o1
	mov	%psr,%o2
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	cmp	%g4,%o2		! %psr should be unchanged
	check_error(NEXTE,bne)

	mov	%g0,%y
	nop; nop; nop

! udiv 
	set	udiv_dat1,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	mov	%psr,%g4
	udiv	%g1,%g2,%g3
	ld	[%l1+8],%o1
	mov	%psr,%o2
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	cmp	%g4,%o2		! %psr should be unchanged
	check_error(NEXTE,bne)
! udiv 
	set	udiv_dat1,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	mov	%psr,%g4
	udiv	%g1,%g2,%g3
	ld	[%l1+8],%o1
	mov	%psr,%o2
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	cmp	%g4,%o2		! %psr should be unchanged
	check_error(NEXTE,bne)
! sdiv 
	set	sdiv_dat1,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	mov	%psr,%g4
	sdiv	%g1,%g2,%g3
	ld	[%l1+8],%o1
	mov	%psr,%o2
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	cmp	%g4,%o2		! %psr should be unchanged
	check_error(NEXTE,bne)
! sdiv 
	set	sdiv_dat2,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	mov	%psr,%g4
	sdiv	%g1,%g2,%g3
	ld	[%l1+8],%o1
	mov	%psr,%o2
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	cmp	%g4,%o2		! %psr should be unchanged
	check_error(NEXTE,bne)
iccs:
! umulcc 
	set	umulcc_dat1,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	ld      [%l1+8],%g4
	umulcc	%g1,%g2,%g3
	mov	%psr,%o2
	ld	[%l1+12],%o1
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	and	%o2,%o7,%o2
	cmp	%g4,%o2
	check_error(NEXTE,bne)
! umulcc 
	set	umulcc_dat2,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	ld      [%l1+8],%g4
	umulcc	%g1,%g2,%g3
	mov	%psr,%o2
	ld	[%l1+12],%o1
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	and	%o2,%o7,%o2
	cmp	%g4,%o2
	check_error(NEXTE,bne)
! smulcc 
	set	smulcc_dat1,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	ld      [%l1+8],%g4
	smulcc	%g1,%g2,%g3
	mov	%psr,%o2
	ld	[%l1+12],%o1
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	and	%o2,%o7,%o2
	cmp	%g4,%o2
	check_error(NEXTE,bne)
! smulcc 
	set	smulcc_dat2,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	ld      [%l1+8],%g4
	smulcc	%g1,%g2,%g3
	mov	%psr,%o2
	ld	[%l1+12],%o1
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	and	%o2,%o7,%o2
	cmp	%g4,%o2
	check_error(NEXTE,bne)

        mov     %g0,%y
        nop; nop; nop

! udivcc 
	set	udivcc_dat1,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	ld      [%l1+8],%g4
	udivcc	%g1,%g2,%g3
	mov	%psr,%o2
	ld	[%l1+12],%o1
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	and	%o2,%o7,%o2
	cmp	%g4,%o2
	check_error(NEXTE,bne)
! udivcc 
	set	udivcc_dat2,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	ld      [%l1+8],%g4
	udivcc	%g1,%g2,%g3
	mov	%psr,%o2
	ld	[%l1+12],%o1
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	and	%o2,%o7,%o2
	cmp	%g4,%o2
	check_error(NEXTE,bne)
! sdivcc 
	set	sdivcc_dat1,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	ld      [%l1+8],%g4
	sdivcc	%g1,%g2,%g3
	mov	%psr,%o2
	ld	[%l1+12],%o1
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	and	%o2,%o7,%o2
	cmp	%g4,%o2
	check_error(NEXTE,bne)
! sdivcc 
	set	sdivcc_dat2,%l1
	ld	[%l1],%g1	
	ld	[%l1+4],%g2	
	ld      [%l1+8],%g4
	sdivcc	%g1,%g2,%g3
	mov	%psr,%o2
	ld	[%l1+12],%o1
	cmp	%g3,%o1
	check_error(NEXTE,bne)
	and	%o2,%o7,%o2
	cmp	%g4,%o2
	check_error(NEXTE,bne)

stop: b testok; nop
		nop

.seg "data"
.align	8

umul_dat1:
	.word	576
	.word	873729
	.word	0x1dff4240
	.word	0,0,0,0,0

umul_dat2:
	.word	0x08954762
	.word	0x08954762
	.word	0x4bfb8184
	.word   0,0,0,0,0,0

smul_dat1:
	.word	873729
	.word	-1
	.word	-873729
	.word   0,0,0,0,0

smul_dat2:
	.word	0x08954762
	.word   0xfffffdc0
	.word	0xb01f6380
	.word   0,0,0,0,0

udiv_dat1:
	.word	873216
	.word	576
	.word	0x000005ec
	.word   0,0,0,0

udiv_dat2:
	.word	873729
	.word	4373729
	.word	0x000005ec
	.word   0,0,0,0

sdiv_dat1:
	.word	-873729
	.word	1516
	.word	0x002b3883
	.word   0,0,0,0,0

sdiv_dat2:
	.word	873729
	.word	-4373729
	.word	0x00000000
	.word   0,0,0,0,0

umulcc_dat1:
	.word	0x3456
	.word	0x4567
	.word	0x00000000	! psr (icc)
	.word	0x0e303c9a	! result

umulcc_dat2:
	.word	0x123456
	.word	0x5678
	.word	0x00000000	! psr (icc)
	.word	0x261d6c50	! result

smulcc_dat1:
	.word	-1234
	.word	345
	.word	0x00800000	! psr (icc)
	.word	-425730		! result

smulcc_dat2:
	.word	-456789
	.word	7654890
	.word	0x00800000	! psr (icc)
	.word	0xde40f14e	! result

udivcc_dat1:
	.word	123456
	.word	123456
	.word	0
	.word	1

udivcc_dat2:
	.word	123456
	.word	1
	.word	0
	.word	123456

sdivcc_dat1:
	.word	-123456
	.word	123456
	.word	0
	.word	0x000087e4

sdivcc_dat2:
	.word	-123456
	.word	1
	.word	0x00200000
	.word	0x7fffffff

