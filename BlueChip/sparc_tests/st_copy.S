/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: copy.s,v 1.2 91/01/10 14:07:20 rudi Exp $"
	.align 8
.seg "text"

#define ERR_RET %r6
#define ERR_RET_NUM 6

#include "err_incr.h"
#include "err_macro.h"

new_init(copy)

#include "crueltt.S"

#include "err_subr.S"

.seg	"text"
start:
		mov	0xa0, %psr		! CWP = 0 S = 1 ET = 1
		mov	0, %wim			! WIM = 0
		set	address_0, %g1
		mov	%g1, %tbr			! TBR = 0
		nop
		nop
		nop

		set	ldarea, %l0
		set	starea, %l1

		! first copy 64 bytes using ld - st

ldst_r:
		ld	[%l0 + 0], %l2
		st	%l2, [%l1 + 0]
		ld	[%l0 + 4], %l2
		st	%l2, [%l1 + 4]
		ld	[%l0 + 8], %l2
		st	%l2, [%l1 + 8]
		ld	[%l0 + 12], %l2
		st	%l2, [%l1 + 12]
		ld	[%l0 + 16], %l2
		st	%l2, [%l1 + 16]
		ld	[%l0 + 20], %l2
		st	%l2, [%l1 + 20]
		ld	[%l0 + 24], %l2
		st	%l2, [%l1 + 24]
		ld	[%l0 + 28], %l2
		st	%l2, [%l1 + 28]
		ld	[%l0 + 32], %l2
		st	%l2, [%l1 + 32]
		ld	[%l0 + 36], %l2
		st	%l2, [%l1 + 36]
		ld	[%l0 + 40], %l2
		st	%l2, [%l1 + 40]
		ld	[%l0 + 44], %l2
		st	%l2, [%l1 + 44]
		ld	[%l0 + 48], %l2
		st	%l2, [%l1 + 48]
		ld	[%l0 + 52], %l2
		st	%l2, [%l1 + 52]
		ld	[%l0 + 56], %l2
		st	%l2, [%l1 + 56]
		ld	[%l0 + 60], %l2
		st	%l2, [%l1 + 60]

! check for errors
		set     ldarea, %l0
                set     starea, %l1
		set	60,%g1
cont:		ld	[%l0+%g1],%g2
		ld	[%l1+%g1],%g3
		cmp	%g2,%g3
		check_error(NEXTE,bne)
		deccc	4,%g1
		bcc	cont
		nop

		! now do all the loads first, then all the stores
		set ldarea+64, %l0
		set starea+64, %l1

ldst_u:
		ld	[%l0 + 0], %i0
		ld	[%l0 + 4], %i1
		ld	[%l0 + 8], %i2
		ld	[%l0 + 12], %i3
		ld	[%l0 + 16], %i4
		ld	[%l0 + 20], %i5
		ld	[%l0 + 24], %i6
		ld	[%l0 + 28], %i7
		ld	[%l0 + 32], %o0
		ld	[%l0 + 36], %o1
		ld	[%l0 + 40], %o2
		ld	[%l0 + 44], %o3
		ld	[%l0 + 48], %o4
		ld	[%l0 + 52], %o5
		ld	[%l0 + 56], %o6
		ld	[%l0 + 60], %o7

		st	%i0, [%l1 + 0]
		st	%i1, [%l1 + 4]
		st	%i2, [%l1 + 8]
		st	%i3, [%l1 + 12]
		st	%i4, [%l1 + 16]
		st	%i5, [%l1 + 20]
		st	%i6, [%l1 + 24]
		st	%i7, [%l1 + 28]
		st	%o0, [%l1 + 32]
		st	%o1, [%l1 + 36]
		st	%o2, [%l1 + 40]
		st	%o3, [%l1 + 44]
		st	%o4, [%l1 + 48]
		st	%o5, [%l1 + 52]
		st	%o6, [%l1 + 56]
		st	%o7, [%l1 + 60]

! check for errors
		set     ldarea+64, %l0
                set     starea+64, %l1
		set	60,%g1
cont1:		ld	[%l0+%g1],%g2
		ld	[%l1+%g1],%g3
		cmp	%g2,%g3
		check_error(NEXTE,bne)
		deccc	4,%g1
		bcc	cont1
		nop

		! now do it with ldd - std
		set ldarea+128, %l0
		set starea+128, %l1

lddstd_r:
		ldd	[%l0 + 0], %l2
		std	%l2, [%l1 + 0]
		ldd	[%l0 + 8], %l2
		std	%l2, [%l1 + 8]
		ldd	[%l0 + 16], %l2
		std	%l2, [%l1 + 16]
		ldd	[%l0 + 24], %l2
		std	%l2, [%l1 + 24]
		ldd	[%l0 + 32], %l2
		std	%l2, [%l1 + 32]
		ldd	[%l0 + 40], %l2
		std	%l2, [%l1 + 40]
		ldd	[%l0 + 48], %l2
		std	%l2, [%l1 + 48]
		ldd	[%l0 + 56], %l2
		std	%l2, [%l1 + 56]

! check for errors
                set     ldarea+128, %l0
                set     starea+128, %l1
                set     60,%g1  
cont2:          ld      [%l0+%g1],%g2
                ld      [%l1+%g1],%g3
                cmp     %g2,%g3 
                check_error(NEXTE,bne)
                deccc   4,%g1   
                bcc     cont2
                nop

		! now do all the ldd's first
		set ldarea+192, %l0
		set starea+192, %l1

lddstd_u:
		ldd	[%l0 + 0], %i0
		ldd	[%l0 + 8], %i2
		ldd	[%l0 + 16], %i4
		ldd	[%l0 + 24], %i6
		ldd	[%l0 + 32], %o0
		ldd	[%l0 + 40], %o2
		ldd	[%l0 + 48], %o4
		ldd	[%l0 + 56], %o6

		std	%i0, [%l1 + 0]
		std	%i2, [%l1 + 8]
		std	%i4, [%l1 + 16]
		std	%i6, [%l1 + 24]
		std	%o0, [%l1 + 32]
		std	%o2, [%l1 + 40]
		std	%o4, [%l1 + 48]
		std	%o6, [%l1 + 56]

! check for errors
                set     ldarea+192, %l0
                set     starea+192, %l1
                set     60,%g1  
cont3:          ld      [%l0+%g1],%g2
                ld      [%l1+%g1],%g3
                cmp     %g2,%g3 
                check_error(NEXTE,bne)
                deccc   4,%g1   
                bcc     cont3
                nop

stop: b testok; nop
		mov	%i6, %i7

.seg "data"
.align	8
	LDAREA_WORDS=64
ldarea:		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
		.word	0x12345678
		.word	0x92b4d6f8
		.word	0x00345e78
		.word	0x3244ddf8
.seg "bss"

.align 8
starea:	.skip 4096
