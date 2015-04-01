/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: ldst.s,v 1.2 91/01/11 11:48:46 rudi Exp $"
.align 8
.seg "text"
                      /* ldst.s */

/*this programs tests load and store type of instructions.		*/
/*The program first stores patterns from register file into memory	*/
/*It then reads them and compares them with the original patterns 	*/
/*Mismatches are flagged down. Register R8 is used to log load errors	*/
/*and R9 to log store errors. At the end of this test:			*/
/**************************************************************************
PSR: 0x000080a2   icc DF PIL S pS ET CWP    WIM
                  0x4  0 0x0 1  0  1 0x02   0x0
                   Z                      00000000

FSR: 0x00000000    RD   RP NXE  pr ftt qne fcc cexc
                   0x0 0x0  0   0  0x0  0  0x0 0x00
                  near  x          NONE     =     

             INS          LOCALS            OUTS         GLOBALS     
  0:  0xffff8833      0x88332211      0x00000000      0x00000000     
  1:  0x00330000      0xffffff88      0x00000000      0x11111111     
  2:  0x88000000      0x88332211      0x88332211      0x22222222     
  3:  0xa34d0ce4      0xffff8833      0x22221111      0x33333333     
  4:  0xa34d0ce4      0x00000088      0x11111111      0x88888888     
  5:  0xa34d0ce4      0xa34d0ce4      0xa34d0ce4      0x88332211     
  6:  0xa34d0ce4      0xa34d0ce4      0xa34d0ce4      0x22221111     
  7:  0xa34d0ce4      0xa34d0ce4      0xa34d0ce4      0x000012c8
***************************************************************************/


/* macro for checking errors */


/* make %o6 the error return reg */
#define ERR_RET %r14
#define ERR_RET_NUM 14

#include "err_incr.h"
#include "err_macro.h"

/************************************************************************

      TRAP TABLE AND ERROR REPORTING SUBROUTINE

************************************************************************/

new_init(ldst)
	
#include "crueltt.S"
	
#include "err_subr.S"


/*************************************************************************/



/* PC trace: 							*/

/*   #include "regdefs.h"    don't need; added % to registers   */                                  

start:		mov	0xa2,	%psr		/*cwp = 2, s = 1, et = 1 */
		mov	0x0,	%wim
	set address_0, %g1
	mov	%g1,	%tbr	
/*generate constants	*/

		sethi	%hi(0x11111111), %r1
		add	%r1, %lo(0x11111111), %r1
		sethi	%hi(0x22222222), %r2
		add	%r2, %lo(0x22222222), %r2
		sethi	%hi(0x33333333), %r3
		add	%r3, %lo(0x33333333), %r3
		sethi	%hi(0x88888888), %r4
		add	%r4, %lo(0x88888888), %r4
		sethi	%hi(0x88332211),%r5
		add	%r5, %lo(0x88332211), %r5
		sethi	%hi(0x22221111), %r6
		add	%r6, %lo(0x22221111), %r6
		set	stoadd,	%r7
		mov	%r0,%r8
		mov     %r0,%r9

stores:		stb	%r4,	[%r7]	
		stb	%r3,	[%r7+1]
		stb	%r2,	[%r7+2]
		stb	%r1,	[%r7+3]	/*4 insts. store 88332211 at stoadd*/
		
		sth	%r2,	[%r7+4]
		sth	%r1,	[%r7+6]	/*store 22221111 in stoadd+4 location*/

		st	%r1,	[%r7+8]	/*stores 11111111 in stoadd+8 loc  */

		/*****************************************
		st	%f1,	[%r7+12]	!introduced to test i-unit seq
		st	%fsr,	[%r7+16]	
		std	%fq,	[%r7+24]
		std	%f2,	[%r7+24]
		*****************************************/

st_test1:	ld	[%r7],	%r10
/*		nop			put as ld,ld+1 conf.not working yet*/
		subcc	%r10, %r5, %r0
		bne,a	st_test2
		or	%r8, 0x1, %r8
st_test2:	ld	[%r7+4], %r11
/*		nop			put as ld,ld+1 conf.not working yet*/
		subcc	%r11, %r6, %r0
		bne,a	st_test3
		or	%r8, 0x2, %r8
st_test3:	ld	[%r7+8],  %r12
/*		nop			put as ld,ld+1 conf.not working yet*/
		subcc	%r12,  %r1,  %r0
		bne,a	loads
		or	%r8, 0x4, %r8
/*by the end of st_tests registers in window 0 should have following values */
/* %r10 = 88332211 */
/* %r11 = 22221111 */
/* %r12 = 11111111 */
/* if %r8(7:0) = 00 then stores work */
! check for errors
		set	0x88332211,%r16
		cmp	%r16,%r10
		check_error(NEXTE,bne)

		set	0x22221111,%r16
		cmp	%r16,%r11
		check_error(NEXTE,bne)

		set	0x11111111,%r16
		cmp	%r16,%r12
		check_error(NEXTE,bne)

		set	0x00ff,%r16
		and	%r16,%r8,%r16
		cmp	%r16,%r0
		check_error(NEXTE,bne)


loads:		ldub	[%r7+3],	%r16	/*%r16 = 00000011*/
		ldub	[%r7+2],	%r24	/*%r24 = 00000022*/
		ldub	[%r7+1],	%r25	/*%r25 = 00000033*/
		ldub	[%r7+0],	%r26	/*%r26 = 00000088*/
		
		sll	%r24, 0x8,  %r24
		sll	%r25, 0x10, %r25
		sll	%r26, 0x18, %r26
		or	%r16,  %r24,  %r16
		or 	%r16,  %r25,  %r16
		or 	%r16,  %r26,  %r16

		subcc	%r16,  %r5,   %r0
		bne,a	ld_test2
		or	%r9,  0x1, %r9
/* by the end of this test %r16 = 88332211 */
! check for errors
		set	0x88332211,%r17
		cmp	%r16,%r17
		check_error(NEXTE,bne)

/* tests ldsb */

ld_test2:	ldsb	[%r7+0], %r17		/*%r17 = ffffff88*/
		sethi	%hi(0xffffff88), %r24
		add	%r24, %lo(0xffffff88), %r24
		subcc	%r17,%r24,%r0
		bne,a	ld_test3
		or	%r9, 0x2, %r9
/* by the end of this test r17 = ffffff88 */

! check for errors 
		set	0xffffff88,%r18
		cmp	%r17,%r18
		check_error(NEXTE,bne)

/* test lduh */

ld_test3:	lduh	[%r7+2], %r18		/*%r18 = 00002211 */
		lduh	[%r7+0], %r24		/*%r24 = 00008833 */

		nop			/*put as ld,ld+1 conf.not working yet*/
		sll	%r24, 0x10, %r24
		or	%r18, %r24, %r18	/*%r18 = 88332211 */
		
		subcc	%r18, %r5, %r0
		bne,a	ld_test4
		or	%r9, 0x4, %r9
/* by the end of this test %r18 = 88332211 */
! check for errors  
		set	0x88332211,%r19
		cmp	%r19,%r18
		check_error(NEXTE,bne)


/* test ldsh */

ld_test4:	ldsh	[%r7+0], %r19		/*%r19 = ffff8833 */
		
		sethi	%hi(0xffff8833), %r24
		add	%r24, %lo(0xffff8833), %r24

		subcc	%r19, %r24, %r0	
		bne,a	ld_test5
		or	%r9, 0x8, %r9
/* at the end of this test %r19 = ffff8833 */
! check for errors   	
		set	0xffff8833,%r20
		cmp	%r19,%r20
		check_error(NEXTE,bne)


/* test ldf, ldfd (for checking i unit sequencing only */

ld_test5:	
		/**************************
		ld	[%r7+0], f1
		ldd	[%r7+0], f2
		**************************/

ldst_test:	ldstub	[%r7], %r20

		ld	[%r7],%g1
		set	0xff332211,%g2
		cmp	%g1,%g2
		check_error(NEXTE,bne)

		set	0x88,%g1
		cmp	%g1,%r20
		check_error(NEXTE,bne)

stop: b testok; nop
		nop

/* at the end of this test %r20 = 88 */
/*		location [%r7]  = ff */
		.align	8
stoadd:		.word 0x00000000, 0x00000000, 0x00000000
