/*
Added header information. can be retrived from binaries w/ ident <filename>
*/
.seg "data"
rcsid: 
        .ascii "$Id: regfile.s,v 1.2 91/01/14 15:11:18 rudi Exp $"
.align 8
.seg "text"

 ! This program tests the register window by writing and reading back zeros and 
 ! ones into all registers.   

#define ERR_RET %r7
#define ERR_RET_NUM 7

#include "err_incr.h"
#include "err_macro.h"

new_init(regfile)

	
#include "crueltt.S"

#include "err_subr.S"

start:

           set     0xe0, %g1
           mov     %g1, %psr          !cwp=0, s=1, ps=1, et=1
           mov     %g0, %wim          !wim=0
	   set     address_0, %g1
           mov     %g1, %tbr          !tbr=address_0
           nop

	   mov	0, %g7			! ERROR Reporting register

 !***********************  Test all window registers  *****************************
 ! Write and read back '0000...00' pattern
 !*********************************************************************************

 wrz_loop: 
           mov     %r0, %r8
           mov     %r0, %r9
           mov     %r0, %r10
           mov     %r0, %r11
           mov     %r0, %r12
           mov     %r0, %r13
           mov     %r0, %r14
           mov     %r0, %r15
           mov     %r0, %r16
           mov     %r0, %r17
           mov     %r0, %r18
           mov     %r0, %r19
           mov     %r0, %r20
           mov     %r0, %r21
           mov     %r0, %r22
           mov     %r0, %r23
  
	   rd		%psr, %g1
	   and		%g1, 0x1f, %g1
	   cmp		%g1, 1
           bne,a     wrz_loop
           save                  !change to window (cwp-1)

	   ! fall out of here in window 1 - only window 0 can be trashed
	   ! by trap PC's
           
           mov     0x0, %g2           !reset the sum counter
           nop

 rdz_loop:
           add     %g2, %r8, %g2
           add     %g2, %r9, %g2
           add     %g2, %r10, %g2
           add     %g2, %r11, %g2
           add     %g2, %r12, %g2
           add     %g2, %r13, %g2
           add     %g2, %r14, %g2
           add     %g2, %r15, %g2
           add     %g2, %r16, %g2
           add     %g2, %r17, %g2
           add     %g2, %r18, %g2
           add     %g2, %r19, %g2
           add     %g2, %r20, %g2
           add     %g2, %r21, %g2
           add     %g2, %r22, %g2
           add     %g2, %r23, %g2

           cmp     %g2, 0
           bne     rdz_error
           nop
           
           restore                  !change to window (cwp+1)
	   rd		%psr, %g1
	   andcc	%g1, 0x1f, %g1
           bnz		rdz_loop
           nop

	   ! drop out of here in window 0 - all windows may now be trashed
	   ! with trap PC's

 ! Write and read back '1111...11' pattern
 !*********************************************************************************


           mov     -1, %r3            !set %r3 to '1111....11'
 wro_loop:
           mov     %r3, %r8
           mov     %r3, %r9
           mov     %r3, %r10
           mov     %r3, %r11
           mov     %r3, %r12
           mov     %r3, %r13
           mov     %r3, %r14
           mov     %r3, %r15
           mov     %r3, %r16
           mov     %r3, %r17
           mov     %r3, %r18
           mov     %r3, %r19
           mov     %r3, %r20
           mov     %r3, %r21
           mov     %r3, %r22
           mov     %r3, %r23
  
	   rd		%psr, %g1
	   and		%g1, 0x1f, %g1
	   cmp		%g1, 1
           bne,a     wro_loop
           save                  !change to window (cwp-1)

	   ! fall out of here in window 1 - only window 0 can be trashed
	   ! by trap PC's
           
!**************** read all registers and check if '1111...11'

           mov     -1, %r2            !%r2 = '1111...11'

 rdo_loop:
           and     %r2, %r8, %r2
           and     %r2, %r9, %r2
           and     %r2, %r10, %r2
           and     %r2, %r11, %r2
           and     %r2, %r12, %r2
           and     %r2, %r13, %r2
           and     %r2, %r14, %r2
           and     %r2, %r15, %r2
           and     %r2, %r16, %r2
           and     %r2, %r17, %r2
           and     %r2, %r18, %r2
           and     %r2, %r19, %r2
           and     %r2, %r20, %r2
           and     %r2, %r21, %r2
           and     %r2, %r22, %r2
           and     %r2, %r23, %r2

           cmp     %r2, -1
           bne     rdo_error
           nop
           
           restore                  !change to window (cwp+1)
	   rd		%psr, %r1
	   andcc	%r1, 0x1f, %r1
           bnz     rdo_loop
           nop

	! now test window 0

w0_test:

           mov     %r0, %r8
           mov     %r0, %r9
           mov     %r0, %r10
           mov     %r0, %r11
           mov     %r0, %r12
           mov     %r0, %r13
           mov     %r0, %r14
           mov     %r0, %r15
           mov     %r0, %r16
           mov     %r0, %r17
           mov     %r0, %r18
           mov     %r0, %r19
           mov     %r0, %r20
           mov     %r0, %r21
           mov     %r0, %r22
           mov     %r0, %r23

	   mov	   0, %g2

           add     %g2, %r8, %g2
           add     %g2, %r9, %g2
           add     %g2, %r10, %g2
           add     %g2, %r11, %g2
           add     %g2, %r12, %g2
           add     %g2, %r13, %g2
           add     %g2, %r14, %g2
           add     %g2, %r15, %g2
           add     %g2, %r16, %g2
           add     %g2, %r17, %g2
           add     %g2, %r18, %g2
           add     %g2, %r19, %g2
           add     %g2, %r20, %g2
           add     %g2, %r21, %g2
           add     %g2, %r22, %g2
           add     %g2, %r23, %g2

	   cmp	%g2, 0
	   bne	rdz_error
	   nop

           mov     -1, %r8
           mov     -1, %r9
           mov     -1, %r10
           mov     -1, %r11
           mov     -1, %r12
           mov     -1, %r13
           mov     -1, %r14
           mov     -1, %r15
           mov     -1, %r16
           mov     -1, %r17
           mov     -1, %r18
           mov     -1, %r19
           mov     -1, %r20
           mov     -1, %r21
           mov     -1, %r22
           mov     -1, %r23

	   mov	-1, %g2

           and     %r2, %r8, %r2
           and     %r2, %r9, %r2
           and     %r2, %r10, %r2
           and     %r2, %r11, %r2
           and     %r2, %r12, %r2
           and     %r2, %r13, %r2
           and     %r2, %r14, %r2
           and     %r2, %r15, %r2
           and     %r2, %r16, %r2
           and     %r2, %r17, %r2
           and     %r2, %r18, %r2
           and     %r2, %r19, %r2
           and     %r2, %r20, %r2
           and     %r2, %r21, %r2
           and     %r2, %r22, %r2
           and     %r2, %r23, %r2

	   cmp	%g2, -1
	   bne	rdo_error
	   nop


           b       stop
           nop
                    

rdz_error:   
	check_error(NEXTE,ba)
	b	stop
	mov	-1, %g7

rdo_error:
	check_error(NEXTE,ba)
	b	stop
	mov	-2, %g7

           
stop: b testok; nop
           nop
