/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: branches.s,v 1.4 91/08/19 08:50:46 rudi Exp $"
.align 8
.seg "text"

! This is a diagnostic for subcc and branch instructions
! It tests a taken and an untaken version of each conditional branch

#define ERR_RET %r7
#define ERR_RET_NUM 7

#include "err_incr.h"
#include "err_macro.h"

new_init(branches)
#include "crueltt.S"
#include "err_subr.S"

#include "su_super.S"


main:
		save	%sp, -16*4, %sp		! next window, establish %sp


! setup some useful values in global registers

#define C0	%g0
#define C1	%g1
		mov	1, %g1

#define C2	%g2
		mov	2, %g2

#define Cmaxneg	%g3
		sethi	%hi(0x80000000), %g3
		or	%g3, %lo(0x80000000), %g3

#define Cm1	%g4
		mov	-1, %g4

#define Cm2	%g5
		mov	-2, %g5

#define Cmax	%g6
		sethi	%hi(0x7fffffff), %g6
		or	%g6, %lo(0x7fffffff), %g6


! Local register 0 will be used to keep track of the success/failure
! of the diagnostic. At the end, it should contain -1, as each of
! the 16 branch possibilities will set a bit when it completes its
! taken branch, and when it completes its untaken branch.
!
!		branch	taken	untaken
!		-----------------------
!		bne	bit 0	bit 1
!		be	bit 2	bit 3
!		bg	bit 4	bit 5
!		ble	bit 6	bit 7
!		bge	bit 8	bit 9
!		bl	bit 10	bit 11
!		bgu	bit 12	bit 13
!		bleu	bit 14	bit 15
!		blu	bit 16	bit 17
!		bgeu	bit 18	bit 19
!		bpos	bit 20	bit 21
!		bneg	bit 22	bit 23
!		bvc	bit 24	bit 25
!		bvs	bit 26	bit 27
!		bn	bit 28	bit 39
!		b	bit 30	bit 31



		mov	0, %l0

! Local Register 1 will contain a 1 in the bit position which is currently
! a candidate to be set.

		mov	1, %l1

! Failure macro:
#define FAIL(next)	sll %l1, 1, %l1; b next; nop

! Success macro:
#define PASS(next)		or %l1, %l0, %l0; sll %l1, 1, %l1; b next; nop

test_bneT:	cmp	Cmaxneg, C0
		bne	bneT_passed
		nop
		FAIL(test_bneU)
bneT_passed:	PASS(test_bneU)
test_bneU:	cmp	Cmaxneg, Cmaxneg
		bne	bneU_failed
		nop
		PASS(test_beT)
bneU_failed:	FAIL(test_beT)

test_beT:	cmp	Cmax, Cmax
		be	beT_passed
		nop
		FAIL(test_beU)
beT_passed:	PASS(test_beU)
test_beU:	cmp	Cmaxneg, C0
		be	beU_failed
		nop
		PASS(test_bgT)
beU_failed:	FAIL(test_bgT)

test_bgT:	cmp	Cmax, Cmaxneg
		bg	bgT_passed
		nop
		FAIL(test_bgU)
bgT_passed:	PASS(test_bgU)
test_bgU:	cmp	Cmaxneg, Cmaxneg
		bg	bgU_failed
		nop
		PASS(test_bleT)
bgU_failed:	FAIL(test_bleT)

test_bleT:	cmp	Cmax, Cmax
		ble	bleT_passed
		nop
		FAIL(test_bleU)
bleT_passed:	PASS(test_bleU)
test_bleU:	cmp	Cm1, Cm2
		ble	bleU_failed
		nop
		PASS(test_bgeT)
bleU_failed:	FAIL(test_bgeT)

test_bgeT:	cmp	C0, C0
		bge	bgeT_passed
		nop
		FAIL(test_bgeU)
bgeT_passed:	PASS(test_bgeU)
test_bgeU:	cmp	Cm2, Cm1
		bge	bgeU_failed
		nop
		PASS(test_blT)
bgeU_failed:	FAIL(test_blT)

test_blT:	cmp	C0, Cmax
		bl	blT_passed
		nop
		FAIL(test_blU)
blT_passed:	PASS(test_blU)
test_blU:	cmp	Cmax, Cmax
		bl	blU_failed
		nop
		PASS(test_bguT)
blU_failed:	FAIL(test_bguT)

test_bguT:	cmp	Cm1, C0
		bgu	bguT_passed
		nop
		FAIL(test_bguU)
bguT_passed:	PASS(test_bguU)
test_bguU:	cmp	C0, C0
		bgu	bguU_failed
		nop
		PASS(test_bleuT)
bguU_failed:	FAIL(test_bleuT)

test_bleuT:	cmp	C0, C0
		bleu	bleuT_passed
		nop
		FAIL(test_bleuU)
bleuT_passed:	PASS(test_bleuU)
test_bleuU:	cmp	C1, C0
		bleu	bleuU_failed
		nop
		PASS(test_bluT)
bleuU_failed:	FAIL(test_bluT)

test_bluT:	cmp	C0, C1
		blu	bluT_passed
		nop
		FAIL(test_bluU)
bluT_passed:	PASS(test_bluU)
test_bluU:	cmp	C0, C0
		blu	bluU_failed
		nop
		PASS(test_bgeuT)
bluU_failed:	FAIL(test_bgeuT)

test_bgeuT:	cmp	C0, C0
		bgeu	bgeuT_passed
		nop
		FAIL(test_bgeuU)
bgeuT_passed:	PASS(test_bgeuU)
test_bgeuU:	cmp	C0, Cmax
		bgeu	bgeuU_failed
		nop
		PASS(test_bposT)
bgeuU_failed:	FAIL(test_bposT)

test_bposT:	addcc	Cmaxneg, Cmaxneg, %g0
		bpos	bposT_passed
		nop
		FAIL(test_bposU)
bposT_passed:	PASS(test_bposU)
test_bposU:	addcc	Cmaxneg, Cmax, %g0
		bpos	bposU_failed
		nop
		PASS(test_bnegT)
bposU_failed:	FAIL(test_bnegT)

test_bnegT:	subcc	Cm2, Cm1, %g0
		bneg	bnegT_passed
		nop
		FAIL(test_bnegU)
bnegT_passed:	PASS(test_bnegU)
test_bnegU:	subcc	C0, C0, %g0
		bneg	bnegU_failed
		nop
		PASS(test_bvcT)
bnegU_failed:	FAIL(test_bvcT)

test_bvcT:	addcc	C0, Cm1, %g0
		bvc	bvcT_passed
		nop
		FAIL(test_bvcU)
bvcT_passed:	PASS(test_bvcU)
test_bvcU:	addcc	Cmaxneg, Cm1, %g0
		bvc	bvcU_failed
		nop
		PASS(test_bvsT)
bvcU_failed:	FAIL(test_bvsT)

test_bvsT:	addcc	Cmax, C1, %g0
		bvs	bvsT_passed
		nop
		FAIL(test_bvsU)
bvsT_passed:	PASS(test_bvsU)
test_bvsU:	addcc	Cmaxneg, Cmax, %g0
		bvs	bvsU_failed
		nop
		PASS(test_bnT)
bvsU_failed:	FAIL(test_bnT)

test_bnT:	addcc	Cmaxneg, Cmax, %g0
		bn	bnU_failed1
		nop
		PASS(test_bnU)
bnU_failed1:	FAIL(test_bnU)
test_bnU:	addcc	Cmaxneg, Cmax, %g0
		bn	bnU_failed2
		nop
		PASS(test_bT)
bnU_failed2:	FAIL(test_bT)

test_bT:	addcc	Cmax, C1, %g0
		b	bT_passed1
		nop
		FAIL(test_bU)
bT_passed1:	PASS(test_bU)
test_bU:	addcc	Cmax, C1, %g0
		b	bT_passed2
		nop
		FAIL(stop1)
bT_passed2:	PASS(stop1)

			! at this point, %l0 should be -1 if all tests passed
stop1:
! check errors
		set	-1,%g1
		cmp	%g1,%l0
		check_error(NEXTE,bne)

stop: b testok; nop
		b	stop

		ret
		restore
