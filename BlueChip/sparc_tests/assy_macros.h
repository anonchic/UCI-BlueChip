#define G0	0
#define G1	1
#define G2	2
#define G3	3
#define G4	4
#define G5	5
#define G6	6
#define G7	7

#define O0	8
#define O1	9
#define O2	10
#define O3	11
#define O4	12
#define O5	13
#define O6	14
#define O7	15

#define L0	16
#define L1	17
#define L2	18
#define L3	19
#define L4	20
#define L5	21
#define L6	22
#define L7	23

#define I0	24
#define I1	25
#define I2	26
#define I3	27
#define I4	28
#define I5	29
#define I6	30
#define I7	31

#define UNIMP	.word (0x00000000)

#define LDD(rs1, rs2, rd) \
	.word (0xc0180000 + ((rd)<<25) + ((rs1)<<14) + (rs2))

#define LDDi(rs1, simm13, rd) \
	.word (0xc0182000 + ((rd)<<25) + ((rs1)<<14) + (simm13))

#define LDDA(rs1, rs2, asi, rd) \
	.word (0xc0980000 + ((rd)<<25) + ((rs1)<<14) + ((asi)<<5) + (rs2))

#define STD(rd, rs1, rs2) \
	.word (0xc0380000 + ((rd)<<25) + ((rs1)<<14) + (rs2))

#define STDi(rd, rs1, simm13) \
	.word (0xc0382000 + ((rd)<<25) + ((rs1)<<14) + (simm13))

#define STDA(rd, asi, rs1, rs2) \
	.word (0xc0b80000 + ((rd)<<25) + ((rs1)<<14) + ((asi)<<5) + (rs2))

#define LDSTB(rs1, rs2, rd) \
	.word (0xc0680000 + ((rd)<<25) + ((rs1)<<14) + (rs2))

#define LDSTBi(rs1, simm13, rd) \
	.word (0xc0682000 + ((rd)<<25) + ((rs1)<<14) + (simm13))

#define LDSTBA(rs1, rs2, asi, rd) \
	.word (0xc0e80000 + ((rd)<<25) + ((rs1)<<14) + ((asi)<<5) + (rs2))

#define SWAP(rs1, rs2, rd) \
	.word (0xc0780000 + ((rd)<<25) + ((rs1)<<14) + (rs2))

#define SWAPi(rs1, simm13, rd) \
	.word (0xc0782000 + ((rd)<<25) + ((rs1)<<14) + (simm13))

#define SWAPA(rs1, rs2, asi, rd) \
	.word (0xc0f80000 + ((rd)<<25) + ((rs1)<<14) + ((asi)<<5) + (rs2))

#define JMP(rs1, rs2, rd) \
	.word (0x81c00000 + ((rd)<<25) + ((rs1)<<14) + (rs2))

#define JMPi(rs1, simm13, rd) \
	.word (0x81c02000 + ((rd)<<25) + ((rs1)<<14) + (simm13))









