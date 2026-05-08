#include "StdAfx.h"
#include "testdef.h"
#include "data.h"
#include "tp3057_reference_data.h"
#include <math.h>

// =====================================================================
// TP3057 contest-scope final test program (Method-A actualization)
// =====================================================================
// Kept in main flow:
// - Required: function, continuity, IIL, IIH, IOZ, ICC0, IBB0, ICC1, IBB1,
//   GXA, GXR, GRA, GRR, GRRL, SFDX, SFDR
// - Optional: IMD (needs TP3057_HAS_AS_PATTERN_API=1 to enable)
// Removed: VOH, VOL, VIL, VIH, VFRO_DC, other non-contest parameters.
//
// ---------------------------------------------------------------------
// Method-A timing policy
// ---------------------------------------------------------------------
// * ATE period remains 244 ns.
// * Active serial window remains 2 rows/bit:
//     row 0: BCLK high, row 1: BCLK low.
// * BCLK active rate remains 2.048 MHz; do not switch the default to
//   4 rows/bit unless the measurement scope is intentionally changed.
// * Default clock relationship is TP3057_CLK_REL_MCLK_LEADS_HALF_CYCLE,
//   but in this 2-row discrete template it is only an approximation:
//   MCLK is placed on the opposite row from BCLK to avoid SAME_PHASE
//   simultaneous switching. It is not a strict continuous-time half-cycle
//   lead until confirmed on scope.
// * The compressed tail is fixed by the mdc LDC/LOOP template. C does not
//   expose an idle-mode switch because the tail is not patched dynamically.
// * TX compare is conservative: only the BCLK-high active row is compared;
//   the adjacent BCLK-low row is patched as X to avoid edge-near sampling.
//
// Theory and layout-assumption checks run on every tp3057() entry via
// tp3057_report_methoda_timing_selfcheck(). No cached pass state is kept,
// so pattern reload/regeneration cannot reuse a stale selfcheck result.
// =====================================================================

#define TP3057_VCC_DPS                       1
#define TP3057_VBB_DPS                       2
#define TP3057_PATTERN_NAME                  "TP3057_MethodA_actual"
#define TP3057_PATTERN_BINARY_NAME           "TP3057_MethodA_actual.mdv"

#define TP3057_DIGITAL_INPUT_PINS            "1,2,3,4,5,6,7"
#define TP3057_DIGITAL_OUTPUT_PINS           "8,9"
#define TP3057_ALL_DIGITAL_PINS              "1,2,3,4,5,6,7,8,9"

#define TP3057_PIN_MCLKR_PDN                 "1"
#define TP3057_PIN_BCLKR_CLKSEL              "2"
#define TP3057_PIN_MCLKX                     "3"
#define TP3057_PIN_BCLKX                     "4"
#define TP3057_PIN_FSX                       "5"
#define TP3057_PIN_FSR                       "6"
#define TP3057_PIN_DR                        "7"
#define TP3057_PIN_DX                        "8"
#define TP3057_PIN_TSX                       "9"

#define TP3057_GXA_NOMINAL_VRMS              1.2276
#define TP3057_GRA_NOMINAL_VRMS              1.2276
#define TP3057_DIST_MEASURE_RANGE_V          4.0
#define TP3057_DIST_RESIDUAL_RANGE_V         0.2
#define TP3057_AVM_RANGE_FULL_V              4.0
#define TP3057_AVM_MEASURE_TDELAY_MS         100
#define TP3057_DELAY_SHORT_MS                5
#define TP3057_DELAY_SETTLE_MS               50
#define TP3057_DELAY_ANALOG_MS               100
#define TP3057_DELAY_POWER_CURRENT_MS        20

#define TP3057_AVM_LPPASS                    0
#define TP3057_AVM_BPPASS                    0
#define TP3057_AVM_BP1K                      1
#define TP3057_AVM_BPNOT                     2
#define TP3057_AVM_TX_MEASURE_CH             1
#define TP3057_AVM_RX_MEASURE_CH             2

#ifndef OFFLINE_SIM
#define OFFLINE_SIM                          0
#endif

// Confirmed ST3020 relay mapping for the current TP3057 adapter board.
// TX AVM path -> relay 31; RX AVM path -> relay 32.
// Override only if the resource-board wiring changes.
#ifndef TP3057_RELAY_AVM_TX
#define TP3057_RELAY_AVM_TX                  "31"
#endif

#ifndef TP3057_RELAY_AVM_RX
#define TP3057_RELAY_AVM_RX                  "32"
#endif

#define TP3057_RELAY_SELECT_EXCLUSIVE        0
#define TP3057_RELAY_SELECT_ACCUMULATE       1

#ifndef TP3057_RELAY_SELECT_MODE
#define TP3057_RELAY_SELECT_MODE             TP3057_RELAY_SELECT_EXCLUSIVE
#endif

#define TP3057_PI                            3.14159265358979323846

#define TP3057_IMD_DVM_CHANNEL               1
#define TP3057_IMD_DVM_RANGE_V               5.0
#define TP3057_IMD_DVM_POINTS                1024
#define TP3057_IMD_DVM_FREQ_DIV              1000
#define TP3057_PATTERN_PIN_WIDTH_BYTES       8
#define TP3057_PATTERN_LINE_BYTES            (TP3057_PATTERN_PIN_WIDTH_BYTES * 3)
// === Method-A actual timing controls ==================================
// Keep SET_PERIOD at 244 ns. Method-A active serial data uses exactly
// 2 rows/bit: one BCLK-high row and one BCLK-low row. The 496-cycle
// tail is a fixed mdc LDC/LOOP template assumption; C does not provide
// a runtime idle-tail mode switch.
#define TP3057_METHODA_PERIOD_NS             244
#define TP3057_METHODA_TARGET_FRAME_NS       125000
#define TP3057_METHODA_FRAME_TOL_NS          TP3057_METHODA_PERIOD_NS

#define TP3057_CLK_REL_SAME_PHASE            0
#define TP3057_CLK_REL_OPPOSITE_PHASE        1
#define TP3057_CLK_REL_MCLK_LEADS_HALF_CYCLE 2

#ifndef TP3057_METHODA_CLK_REL
#define TP3057_METHODA_CLK_REL               TP3057_CLK_REL_MCLK_LEADS_HALF_CYCLE
#endif

#define TP3057_COMPARE_ALL_ACTIVE_ROWS       0
#define TP3057_COMPARE_STABLE_HIGH_ROW_ONLY  1

#ifndef TP3057_METHODA_COMPARE_MODE
#define TP3057_METHODA_COMPARE_MODE          TP3057_COMPARE_STABLE_HIGH_ROW_ONLY
#endif

#ifndef TP3057_FORCE_PATTERN_RELOAD_EACH_RUN
#define TP3057_FORCE_PATTERN_RELOAD_EACH_RUN 0
#endif

#define TP3057_METHODA_ACTIVE_ROWS_PER_BIT   2

#define TP3057_STREAM_WINDOW_BITS            8
#define TP3057_STREAM_WINDOW_BIT_CYCLES      TP3057_METHODA_ACTIVE_ROWS_PER_BIT
#define TP3057_STREAM_WINDOW_BCLK_HIGH_CYCLES 1
#define TP3057_STREAM_WINDOW_LINES           (TP3057_STREAM_WINDOW_BITS * TP3057_STREAM_WINDOW_BIT_CYCLES)

#define TP3057_START_INDEX_TX_COMPARE        46
#define TP3057_START_INDEX_RX_STREAM         47
#define TP3057_RX_MEASURE_STREAM_START_INDEX 48

#define TP3057_FRAME_TAIL_LDC_N              246
#define TP3057_RX_MEASURE_MDC_FRAMES         160
#define TP3057_RX_MEASURE_STREAM_FRAMES      TP3057_REF_SAMPLE_COUNT
#define TP3057_RX_MEASURE_STREAM_LINES       (TP3057_RX_MEASURE_STREAM_FRAMES * TP3057_STREAM_WINDOW_LINES)
#define TP3057_TX_COMPARE_MEASURE_BITS       0
#define TP3057_RX_STREAM_MEASURE_BITS        0
#define TP3057_LEGACY_BIN_METHODA_LAYOUT_BLOCKED 34

// v7 fix #15: LOAD_AS_PATTERN is a function in testdef.h, NOT a macro.
// `#ifndef LOAD_AS_PATTERN` cannot reliably detect its presence because
// the preprocessor is blind to function declarations.
// Use an explicit build-time switch instead; default OFF (IMD optional).
// To enable: add  -DTP3057_HAS_AS_PATTERN_API=1  to the build command.
#ifndef TP3057_HAS_AS_PATTERN_API
#define TP3057_HAS_AS_PATTERN_API 0
#endif

#ifndef TP3057_IMD_SOURCE_READY
#define TP3057_IMD_SOURCE_READY              0
#endif
#ifndef TP3057_IMD_SOURCE_VRMS
#define TP3057_IMD_SOURCE_VRMS               0.3882
#endif
#ifndef TP3057_IMD_AS_MODE
#define TP3057_IMD_AS_MODE                   8
#endif
#ifndef TP3057_IMD_AS_BANK
#define TP3057_IMD_AS_BANK                   0
#endif
#ifndef TP3057_IMD_AS_FREQ_DIV
#define TP3057_IMD_AS_FREQ_DIV               1953
#endif
#ifndef TP3057_IMD_TONE1_HZ
#define TP3057_IMD_TONE1_HZ                  700.0
#endif
#ifndef TP3057_IMD_TONE2_HZ
#define TP3057_IMD_TONE2_HZ                  1900.0
#endif
#ifndef TP3057_IMD_TABLE_POINTS
#define TP3057_IMD_TABLE_POINTS              2
static WORD g_tp3057_imd_as_bank0[TP3057_IMD_TABLE_POINTS] = {32768,32768};
#endif

static BOOL tp3057_report_missing_reference(CString item);
static BOOL tp3057_report_blocked(CString item);
static void tp3057_show_flag(CString item, BOOL pass);
static BOOL tp3057_run_pattern_checked(int index);
static BOOL tp3057_run_pattern_auto(int index);
static BOOL tp3057_run_pattern_auto(int index,CString item);
static void tp3057_select_avm_tx(void);
static void tp3057_select_avm_rx(void);
static BOOL tp3057_report_methoda_timing_selfcheck(void);

static void tp3057_apply_relay_select(CString relay_no)
{
#if !OFFLINE_SIM
#if TP3057_RELAY_SELECT_MODE == TP3057_RELAY_SELECT_ACCUMULATE
    CLOSE_RELAY(relay_no);
#else
    SET_RELAY(relay_no);
#endif
#endif
}

static void tp3057_setup_common_io(void)
{
    CLEAR_ALL();
    SET_PERIOD(TP3057_METHODA_PERIOD_NS);
    SET_TIMING(61,183,200);
    FORMAT(NRZ0, TP3057_DIGITAL_INPUT_PINS);
    FORMAT(RO, TP3057_DIGITAL_OUTPUT_PINS);
}

static void tp3057_set_safe_digital_levels(void)
{
    SET_INPUT_LEVEL(0.0,0.0);
    SET_OUTPUT_LEVEL(0.0,0.0);
}

static void tp3057_set_active_digital_levels(void)
{
    SET_INPUT_LEVEL(3.5,0.4);
    SET_OUTPUT_LEVEL(2.4,0.4);
}

static void tp3057_select_avm_tx(void)
{
    tp3057_apply_relay_select(TP3057_RELAY_AVM_TX);
}

static void tp3057_select_avm_rx(void)
{
    tp3057_apply_relay_select(TP3057_RELAY_AVM_RX);
}

static BYTE tp3057_map_bin16(int legacy_bin)
{
    if(legacy_bin <= 0){ return 0; }   // pass
    if(legacy_bin == 1){ return 1; }   // continuity: digital pins
    if(legacy_bin <= 3){ return 2; }   // continuity: analog/power pins
    if(legacy_bin <= 10){ return 3; }  // IIL inputs
    if(legacy_bin <= 17){ return 4; }  // IIH inputs
    if(legacy_bin <= 19){ return 5; }  // IOZH/IOZL
    if(legacy_bin <= 21){ return 6; }  // ICC0/IBB0
    if(legacy_bin <= 23){ return 7; }  // ICC1/IBB1
    if(legacy_bin == 24){ return 8; }  // GXA
    if(legacy_bin == 25){ return 9; }  // GXR
    if(legacy_bin == 26){ return 10; } // SFDX
    if(legacy_bin == 27){ return 11; } // FUNC_ENCODE
    if(legacy_bin == 28){ return 12; } // FUNC_DECODE
    if(legacy_bin == 29){ return 13; } // historical function bucket, reserved; not used by current main flow
    if(legacy_bin <= 32){ return 14; } // GRA/GRR/GRRL
    return 15;                         // SFDR and any later reject bucket, including Method-A layout blocked
}

static void tp3057_bin(int legacy_bin)
{
    BIN(tp3057_map_bin16(legacy_bin));
}

static void tp3057_power_off(void)
{
    SET_AS_DC(0.0,V);
    tp3057_set_safe_digital_levels();
    SET_DPS(TP3057_VCC_DPS,0.0,V,20,MA);
    SET_DPS(TP3057_VBB_DPS,0.0,V,20,MA);
    Delay(TP3057_DELAY_SETTLE_MS);
#if !OFFLINE_SIM
    CLEAR_RELAY(TP3057_RELAY_AVM_TX);
    CLEAR_RELAY(TP3057_RELAY_AVM_RX);
#endif
    CLEAR_ALL();
}

static void tp3057_begin_unpowered_block(void)
{
    tp3057_setup_common_io();
    tp3057_set_safe_digital_levels();
    SET_DPS(TP3057_VCC_DPS,0.0,V,20,MA);
    SET_DPS(TP3057_VBB_DPS,0.0,V,20,MA);
    Delay(TP3057_DELAY_SETTLE_MS);
}

static void tp3057_begin_powered_block(void)
{
    tp3057_setup_common_io();
    tp3057_set_safe_digital_levels();
    SET_DPS(TP3057_VBB_DPS,-5.0,V,20,MA);
    Delay(TP3057_DELAY_SETTLE_MS);
    SET_DPS(TP3057_VCC_DPS,5.0,V,20,MA);
    Delay(TP3057_DELAY_SETTLE_MS);
    tp3057_set_active_digital_levels();
    Delay(TP3057_DELAY_SHORT_MS);
}

static BYTE* g_tp3057_pattern_addr = 0;

static BOOL tp3057_ensure_pattern_image(void)
{
    if(g_tp3057_pattern_addr != 0){
        return TRUE;
    }
    g_tp3057_pattern_addr = LOAD_PATTERN(TP3057_PATTERN_BINARY_NAME);
    if(g_tp3057_pattern_addr == 0){
        tp3057_show_flag("PATTERN_BINARY_MDV_READY",FALSE);
        return FALSE;
    }
    tp3057_show_flag("PATTERN_BINARY_MDV_READY",TRUE);
    return TRUE;
}

static DWORD tp3057_start_index_pc(int start_idx)
{
    DWORD temp;
    DWORD pc;

    if(g_tp3057_pattern_addr == 0){
        return 0x7FFFFF;
    }
    if((start_idx < 0) || (start_idx > 255)){
        tp3057_show_flag("PATTERN_START_INDEX_RANGE",FALSE);
        return 0x7FFFFF;
    }

    temp = g_tp3057_pattern_addr[start_idx*3+0x10];
    temp &= 0x7f;
    pc = temp << 8;
    temp = g_tp3057_pattern_addr[start_idx*3+0x10+1];
    pc = (pc + temp) << 8;
    temp = g_tp3057_pattern_addr[start_idx*3+0x10+2];
    pc += temp;
    if(pc == 0x7FFFFF){
        tp3057_show_flag("PATTERN_PC_SENTINEL",FALSE);
        return 0x7FFFFF;
    }
    return pc;
}

static void tp3057_clear_line_data(BYTE* line_data)
{
    int i;
    for(i=0; i<TP3057_PATTERN_LINE_BYTES; ++i){
        line_data[i] = 0;
    }
}

static void tp3057_set_line_f_bit(BYTE* line_data,int channel)
{
    int byte_idx = (channel - 1) / 8;
    BYTE mask = (BYTE)(1 << ((channel - 1) % 8));
    line_data[byte_idx] |= mask;
}

static void tp3057_set_line_d_bit(BYTE* line_data,int channel)
{
    int byte_idx = (channel - 1) / 8;
    BYTE mask = (BYTE)(1 << ((channel - 1) % 8));
    line_data[TP3057_PATTERN_PIN_WIDTH_BYTES + byte_idx] |= mask;
}

static void tp3057_set_line_m_bit(BYTE* line_data,int channel)
{
    int byte_idx = (channel - 1) / 8;
    BYTE mask = (BYTE)(1 << ((channel - 1) % 8));
    line_data[(TP3057_PATTERN_PIN_WIDTH_BYTES * 2) + byte_idx] |= mask;
}

static void tp3057_set_input_channel(BYTE* line_data,int channel,int bit_value)
{
    tp3057_set_line_d_bit(line_data,channel);
    if(bit_value){
        tp3057_set_line_f_bit(line_data,channel);
    }
}

static void tp3057_set_output_expect(BYTE* line_data,int channel,int bit_value)
{
    tp3057_set_line_m_bit(line_data,channel);
    if(bit_value){
        tp3057_set_line_f_bit(line_data,channel);
    }
}

static BOOL tp3057_build_line_data(const char* vector9,BYTE* line_data)
{
    int i;
    tp3057_clear_line_data(line_data);
    for(i=0; i<9; ++i){
        char c = vector9[i];
        if(c == 0){
            return FALSE;
        }
        if(i < 7){
            if(c == '0'){
                tp3057_set_input_channel(line_data,i+1,0);
            }
            else if(c == '1'){
                tp3057_set_input_channel(line_data,i+1,1);
            }
            else if((c == 'x') || (c == 'X')){
                // Conservative strategy: generated patch rows must actively
                // define DUT input pins. Treat input X as a caller bug instead
                // of silently producing uncertain DUT stimulus.
                return FALSE;
            }
            else{
                return FALSE;
            }
        }
        else{
            if((c == 'h') || (c == 'H')){
                tp3057_set_output_expect(line_data,i+1,1);
            }
            else if((c == 'l') || (c == 'L')){
                tp3057_set_output_expect(line_data,i+1,0);
            }
            else if((c == 'x') || (c == 'X')){
            }
            else{
                return FALSE;
            }
        }
    }
    return TRUE;
}

static BOOL tp3057_build_stream_vector(int mclk_high,int bclk_high,int fsx_high,int fsr_high,int dr_value,char dx_expect,BYTE* line_data)
{
    char vector9[10];

    vector9[0] = '0';
    vector9[1] = '1';
    vector9[2] = mclk_high ? '1' : '0';
    vector9[3] = bclk_high ? '1' : '0';
    vector9[4] = fsx_high ? '1' : '0';
    vector9[5] = fsr_high ? '1' : '0';
    vector9[6] = dr_value ? '1' : '0';
    vector9[7] = dx_expect;
    vector9[8] = 'X';
    vector9[9] = 0;
    return tp3057_build_line_data(vector9,line_data);
}

// =================================================================
// Method-A frame pattern layout  (must match TP3057_MethodA_actual.mdc)
// =================================================================
// Layout assumptions are intentionally explicit. C patches only active
// rows. The tail is a fixed mdc template and is not controlled by an
// idle-mode switch in C.
//
// START_INDEX(46): TX compare single-frame template
//   +0..+15 : active rows patched by C with DX compare expectations
//   +16..+19: fixed LDC/INC/LOOP/final-INC tail rows in mdc
//   +20     : HALT, not part of TP3057_FRAME_PC_STRIDE
//
// START_INDEX(47): RX stream single-frame template
//   +0..+15 : active rows patched by C with DR input bits
//   +16..+19: fixed LDC/INC/LOOP/final-INC tail rows in mdc
//   +20     : HALT, not part of TP3057_FRAME_PC_STRIDE
//
// START_INDEX(48): RX measurement dynamic stream
//   +0      : LDF envelope row
//   frame n : 16 active rows + 4 fixed tail rows, stride = 20 PC slots
//   last frame tail row +19 is JMP,RXM1 instead of final INC
//   final   : HALT, reached only after SET_MASKJMP()
//
// Tail execution model is a theory/layout assumption for ST3020 LDC/LOOP:
//   1 + (LDC_N + 1) * 2 + 1 cycles. It is reported as theory only and must
//   still be confirmed with an oscilloscope on real hardware.
//
// r5 maintenance notes:
// - This C code is not an mdc semantic interpreter. Runtime layout checks
//   verify start-index PC validity/order and the 46->47 / 47->48 spans only.
// - Current mdc has no START_INDEX(49), so the end boundary of 48 cannot be
//   proven from the start-index table. It is reported as boundary-unverified.
// - LDC/LOOP tail execution, 48 tail/JMP runtime boundary, and the discrete
//   MCLK_LEADS_HALF_CYCLE approximation still require scope/hardware verify.
// - TX compare on only the high row is a stability-over-coverage trade-off.
#define TP3057_METHODA_TAIL_THEORY_CYCLES(n)       (1 + (((n) + 1) * 2) + 1)

#define TP3057_IDX46_ACTIVE_ROWS                   (TP3057_STREAM_WINDOW_LINES)
#define TP3057_IDX46_TAIL_ROWS                     4
#define TP3057_IDX46_FRAME_STRIDE                  (TP3057_IDX46_ACTIVE_ROWS + TP3057_IDX46_TAIL_ROWS)
#define TP3057_IDX46_TOTAL_PC_ROWS                 (TP3057_IDX46_FRAME_STRIDE + 1)

#define TP3057_IDX47_ACTIVE_ROWS                   (TP3057_STREAM_WINDOW_LINES)
#define TP3057_IDX47_TAIL_ROWS                     4
#define TP3057_IDX47_FRAME_STRIDE                  (TP3057_IDX47_ACTIVE_ROWS + TP3057_IDX47_TAIL_ROWS)
#define TP3057_IDX47_TOTAL_PC_ROWS                 (TP3057_IDX47_FRAME_STRIDE + 1)

#define TP3057_IDX48_LDF_ROWS                      1
#define TP3057_IDX48_FRAME_DATA_ROWS               (TP3057_STREAM_WINDOW_LINES)
#define TP3057_IDX48_FRAME_TAIL_ROWS               4
#define TP3057_IDX48_FRAME_STRIDE                  (TP3057_IDX48_FRAME_DATA_ROWS + TP3057_IDX48_FRAME_TAIL_ROWS)

#define TP3057_FRAME_DATA_ROWS                     TP3057_IDX48_FRAME_DATA_ROWS
#define TP3057_FRAME_TAIL_SOURCE_ROWS              TP3057_IDX48_FRAME_TAIL_ROWS
#define TP3057_FRAME_PC_STRIDE                     TP3057_IDX48_FRAME_STRIDE
#define TP3057_RX_MEASURE_LDF_ROWS                 TP3057_IDX48_LDF_ROWS
#define TP3057_FRAME_ACTIVE_CYCLES                 TP3057_FRAME_DATA_ROWS
#define TP3057_FRAME_TAIL_CYCLES                   TP3057_METHODA_TAIL_THEORY_CYCLES(TP3057_FRAME_TAIL_LDC_N)
#define TP3057_FRAME_TOTAL_CYCLES                  (TP3057_FRAME_ACTIVE_CYCLES + TP3057_FRAME_TAIL_CYCLES)
#define TP3057_FRAME_TOTAL_NS                      (TP3057_FRAME_TOTAL_CYCLES * TP3057_METHODA_PERIOD_NS)
#define TP3057_FRAME_DEV_NS                        (TP3057_FRAME_TOTAL_NS - TP3057_METHODA_TARGET_FRAME_NS)

typedef char tp3057_check_stride[
    (TP3057_IDX46_ACTIVE_ROWS == 16 &&
     TP3057_IDX47_ACTIVE_ROWS == 16 &&
     TP3057_IDX48_FRAME_DATA_ROWS == 16 &&
     TP3057_IDX46_FRAME_STRIDE == 20 &&
     TP3057_IDX47_FRAME_STRIDE == 20 &&
     TP3057_IDX48_FRAME_STRIDE == 20 &&
     TP3057_FRAME_TOTAL_CYCLES == 512) ? 1 : -1];

typedef char tp3057_check_rx_measure_frame_count[
    (TP3057_RX_MEASURE_STREAM_FRAMES == TP3057_RX_MEASURE_MDC_FRAMES) ? 1 : -1];

typedef struct tag_tp3057_methoda_timing {
    int mclk_high;
    int bclk_high;
    int fsx_high;
    int fsr_high;
    int dr_data_valid_window;
    int compare_enable;
} TP3057_METHODA_TIMING;

static int tp3057_methoda_eval_mclk(int bclk_high,int row_in_bit,int clk_rel)
{
    switch(clk_rel){
    case TP3057_CLK_REL_SAME_PHASE:
        return bclk_high ? 1 : 0;
    case TP3057_CLK_REL_OPPOSITE_PHASE:
        return bclk_high ? 0 : 1;
    case TP3057_CLK_REL_MCLK_LEADS_HALF_CYCLE:
    default:
        // Approximation under the 2-row discrete template: keep MCLK on the
        // opposite row from BCLK. This is deliberately named as a clock
        // relationship choice for compatibility, but it is not a strict
        // continuous-time half-cycle lead until scope-verified.
        return (row_in_bit == 0) ? 0 : 1;
    }
}

static void tp3057_eval_methoda_active_timing(int bit_index,
                                             int row_in_bit,
                                             int clk_rel,
                                             int compare_requested,
                                             TP3057_METHODA_TIMING* timing)
{
    timing->bclk_high = (row_in_bit < TP3057_STREAM_WINDOW_BCLK_HIGH_CYCLES) ? 1 : 0;
    timing->mclk_high = tp3057_methoda_eval_mclk(timing->bclk_high,row_in_bit,clk_rel);
    timing->fsx_high = (bit_index == 0) ? 1 : 0;
    timing->fsr_high = (bit_index == 0) ? 1 : 0;
    timing->dr_data_valid_window = 1;

#if TP3057_METHODA_COMPARE_MODE == TP3057_COMPARE_ALL_ACTIVE_ROWS
    timing->compare_enable = compare_requested ? 1 : 0;
#else
    // Conservative trade-off: compare only on the BCLK-high active row.
    // This sacrifices coverage on the paired low row in exchange for less
    // edge-near sampling risk on a 2-row/bit template.
    timing->compare_enable = (compare_requested && (row_in_bit == 0)) ? 1 : 0;
#endif
}

static DWORD tp3057_pc_46_data_row(DWORD pc,int bit_idx,int row_idx)
{
    return pc + (DWORD)(bit_idx * TP3057_METHODA_ACTIVE_ROWS_PER_BIT + row_idx);
}

static DWORD tp3057_pc_47_data_row(DWORD pc,int bit_idx,int row_idx)
{
    return pc + (DWORD)(bit_idx * TP3057_METHODA_ACTIVE_ROWS_PER_BIT + row_idx);
}

static DWORD tp3057_pc_48_frame_row(DWORD pc,int frame_idx,int row_idx)
{
    return pc + (DWORD)TP3057_IDX48_LDF_ROWS +
           (DWORD)(frame_idx * TP3057_IDX48_FRAME_STRIDE + row_idx);
}

static BOOL tp3057_verify_methoda_mdc_layout(void)
{
    DWORD pc46;
    DWORD pc47;
    DWORD pc48;
    BOOL pc_valid;
    BOOL order_ok;
    BOOL diff46_ok;
    BOOL diff47_ok;
    BOOL idx48_span_ok;
    BOOL idx48_boundary_unverified;
    BOOL partial_layout_ok;
    BOOL full_layout_ok;

    if(!tp3057_ensure_pattern_image()){
        tp3057_show_flag("METHA_MDC_START_PC_VALID",FALSE);
        tp3057_show_flag("METHA_MDC_START_PC_ORDER",FALSE);
        tp3057_show_flag("METHA_MDC_IDX46_SPAN_OK",FALSE);
        tp3057_show_flag("METHA_MDC_IDX47_SPAN_OK",FALSE);
        tp3057_show_flag("METHA_MDC_IDX48_SPAN_OK",FALSE);
        tp3057_show_flag("METHA_MDC_IDX48_BOUNDARY_UNVERIFIED",TRUE);
        tp3057_show_flag("METHA_MDC_LAYOUT_PARTIALLY_VERIFIED",FALSE);
        tp3057_show_flag("METHA_MDC_LAYOUT_FULLY_VERIFIED",FALSE);
        return FALSE;
    }

    pc46 = tp3057_start_index_pc(TP3057_START_INDEX_TX_COMPARE);
    pc47 = tp3057_start_index_pc(TP3057_START_INDEX_RX_STREAM);
    pc48 = tp3057_start_index_pc(TP3057_RX_MEASURE_STREAM_START_INDEX);

    SHOW_RESULT("METHA_MDC_PC_46",(double)pc46,"pc",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_MDC_PC_47",(double)pc47,"pc",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_MDC_PC_48",(double)pc48,"pc",No_UpLimit,No_LoLimit);

    pc_valid = ((pc46 != 0x7FFFFF) && (pc47 != 0x7FFFFF) && (pc48 != 0x7FFFFF)) ? TRUE : FALSE;
    order_ok = (pc_valid && (pc47 > pc46) && (pc48 > pc47)) ? TRUE : FALSE;
    diff46_ok = (order_ok && ((pc47 - pc46) == (DWORD)TP3057_IDX46_TOTAL_PC_ROWS)) ? TRUE : FALSE;
    diff47_ok = (order_ok && ((pc48 - pc47) == (DWORD)TP3057_IDX47_TOTAL_PC_ROWS)) ? TRUE : FALSE;
    idx48_span_ok = FALSE;
    idx48_boundary_unverified = TRUE;
    partial_layout_ok = (pc_valid && order_ok && diff46_ok && diff47_ok) ? TRUE : FALSE;
    full_layout_ok = FALSE;

    tp3057_show_flag("METHA_MDC_START_PC_VALID",pc_valid);
    tp3057_show_flag("METHA_MDC_START_PC_ORDER",order_ok);
    tp3057_show_flag("METHA_MDC_IDX46_SPAN_OK",diff46_ok);
    tp3057_show_flag("METHA_MDC_IDX47_SPAN_OK",diff47_ok);
    tp3057_show_flag("METHA_MDC_IDX48_SPAN_OK",idx48_span_ok);
    tp3057_show_flag("METHA_MDC_IDX48_BOUNDARY_UNVERIFIED",idx48_boundary_unverified);
    tp3057_show_flag("METHA_MDC_LAYOUT_PARTIALLY_VERIFIED",partial_layout_ok);
    tp3057_show_flag("METHA_MDC_LAYOUT_FULLY_VERIFIED",full_layout_ok);
    return partial_layout_ok;
}

static BOOL tp3057_report_methoda_timing_selfcheck(void)
{
    int dev_ns;
    BOOL theory_ok;
    BOOL layout_ok;

    // r6: do not cache a pass result. TP3057_FORCE_PATTERN_RELOAD_EACH_RUN can
    // rebind the pattern image, so theory/layout checks must rerun every entry.
    dev_ns = TP3057_FRAME_DEV_NS;
    theory_ok = ((dev_ns <= TP3057_METHODA_FRAME_TOL_NS) && (dev_ns >= -TP3057_METHODA_FRAME_TOL_NS)) ? TRUE : FALSE;

    SHOW_RESULT("METHA_THEORY_PERIOD_NS",(double)TP3057_METHODA_PERIOD_NS,"ns",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_LAYOUT_ASSUME_ACTIVE_ROWS",(double)TP3057_FRAME_ACTIVE_CYCLES,"cycle",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_LAYOUT_ASSUME_TAIL_ROWS",(double)TP3057_FRAME_TAIL_SOURCE_ROWS,"row",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_LAYOUT_ASSUME_IDX46_ROWS",(double)TP3057_IDX46_TOTAL_PC_ROWS,"pc",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_LAYOUT_ASSUME_IDX47_ROWS",(double)TP3057_IDX47_TOTAL_PC_ROWS,"pc",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_LAYOUT_ASSUME_IDX48_STRIDE",(double)TP3057_IDX48_FRAME_STRIDE,"pc",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_TAIL_LDC_N_THEORY",(double)TP3057_FRAME_TAIL_LDC_N,"count",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_TAIL_CYCLES_THEORY",(double)TP3057_FRAME_TAIL_CYCLES,"cycle",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_THEORY_FRAME_CYCLES",(double)TP3057_FRAME_TOTAL_CYCLES,"cycle",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_THEORY_FRAME_NS",(double)TP3057_FRAME_TOTAL_NS,"ns",
                (double)(TP3057_METHODA_TARGET_FRAME_NS + TP3057_METHODA_FRAME_TOL_NS),
                (double)(TP3057_METHODA_TARGET_FRAME_NS - TP3057_METHODA_FRAME_TOL_NS));
    SHOW_RESULT("METHA_THEORY_FRAME_US",((double)TP3057_FRAME_TOTAL_NS)/1000.0,"us",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_THEORY_FRAME_DEV_NS",(double)dev_ns,"ns",
                (double)TP3057_METHODA_FRAME_TOL_NS,
                (double)(-TP3057_METHODA_FRAME_TOL_NS));
    SHOW_RESULT("METHA_CLK_REL_APPROX_MODE",(double)TP3057_METHODA_CLK_REL,"mode",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_COMPARE_MODE",(double)TP3057_METHODA_COMPARE_MODE,"mode",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_RX_MEAS_FRAMES",(double)TP3057_RX_MEASURE_STREAM_FRAMES,"frame",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_RELAY_MODE",(double)TP3057_RELAY_SELECT_MODE,"mode",No_UpLimit,No_LoLimit);
    SHOW_RESULT("METHA_PATTERN_RELOAD",(double)TP3057_FORCE_PATTERN_RELOAD_EACH_RUN,"mode",No_UpLimit,No_LoLimit);
    tp3057_show_flag("METHA_CLK_REL_DISCRETE_APPROX",
                     (TP3057_METHODA_CLK_REL == TP3057_CLK_REL_MCLK_LEADS_HALF_CYCLE) ? TRUE : FALSE);
    tp3057_show_flag("METHA_COMPARE_HIGH_ONLY",
                     (TP3057_METHODA_COMPARE_MODE == TP3057_COMPARE_STABLE_HIGH_ROW_ONLY) ? TRUE : FALSE);
    tp3057_show_flag("METHA_THEORY_FRAME_WITHIN_TOL",theory_ok);
    tp3057_show_flag("METHA_TAIL_EXEC_MODEL_ASSUMED",TRUE);
    tp3057_show_flag("METHA_TIMING_THEORY_ONLY",TRUE);
    tp3057_show_flag("METHA_SCOPE_NOT_VERIFIED",TRUE);

    layout_ok = tp3057_verify_methoda_mdc_layout();
    if(theory_ok && layout_ok){
        return TRUE;
    }
    return FALSE;
}

static BOOL tp3057_patch_rx_measure_stream(const BYTE* ref_bits,int ref_bit_count,CString item)
{
    BYTE line_data[TP3057_PATTERN_LINE_BYTES];
    TP3057_METHODA_TIMING timing;
    DWORD pc;
    int frame_idx;
    int data_row;
    int bit_index;
    int row_in_bit;
    int ref_bit_index;

    if(ref_bit_count < TP3057_STREAM_WINDOW_BITS){
        return tp3057_report_missing_reference(item + "_MEASURE_BITS_SHORT");
    }
    if(!tp3057_ensure_pattern_image()){
        tp3057_show_flag(item + "_RX_MEASURE_PATCH",FALSE);
        return tp3057_report_blocked(item + "_RX_MEASURE_PATCH_INTERFACE");
    }

    pc = tp3057_start_index_pc(TP3057_RX_MEASURE_STREAM_START_INDEX);
    if(pc == 0x7FFFFF){
        tp3057_show_flag(item + "_RX_MEASURE_PC",FALSE);
        return tp3057_report_blocked(item + "_RX_MEASURE_PC");
    }

    SET_WLD();
    for(frame_idx=0; frame_idx<TP3057_RX_MEASURE_STREAM_FRAMES; ++frame_idx){
        for(data_row=0; data_row<TP3057_FRAME_DATA_ROWS; ++data_row){
            bit_index = data_row / TP3057_STREAM_WINDOW_BIT_CYCLES;
            row_in_bit = data_row % TP3057_STREAM_WINDOW_BIT_CYCLES;
            ref_bit_index = (frame_idx * TP3057_STREAM_WINDOW_BITS) + bit_index;
            ref_bit_index %= ref_bit_count;

            tp3057_eval_methoda_active_timing(bit_index,row_in_bit,TP3057_METHODA_CLK_REL,FALSE,&timing);
            if(!tp3057_build_stream_vector(timing.mclk_high,
                                           timing.bclk_high,
                                           0,
                                           timing.fsr_high,
                                           ref_bits[ref_bit_index] ? 1 : 0,
                                           'X',
                                           line_data)){
                SET_CLRWLD();
                return tp3057_report_blocked(item + "_RX_MEASURE_VECTOR_BUILD");
            }
            SET_PC(tp3057_pc_48_frame_row(pc,frame_idx,data_row));
            LoadData(line_data,TP3057_PATTERN_LINE_BYTES);
        }
    }
    SET_CLRWLD();
    tp3057_show_flag(item + "_RX_MEASURE_PATCH",TRUE);
    return TRUE;
}

static BOOL tp3057_bind_tx_compare_chunk(const BYTE* ref_bits,int ref_bit_count,CString item)
{
    BYTE line_data[TP3057_PATTERN_LINE_BYTES];
    TP3057_METHODA_TIMING timing;
    DWORD pc;
    int data_row;
    int bit_index;
    int row_in_bit;
    char dx_expect;

    if(ref_bit_count < TP3057_STREAM_WINDOW_BITS){
        return tp3057_report_missing_reference(item + "_BITS_SHORT");
    }
    if(!tp3057_ensure_pattern_image()){
        tp3057_show_flag(item + "_TX_STREAM_PATCH",FALSE);
        return tp3057_report_blocked(item + "_TX_PATCH_INTERFACE");
    }
    pc = tp3057_start_index_pc(TP3057_START_INDEX_TX_COMPARE);
    if(pc == 0x7FFFFF){
        tp3057_show_flag(item + "_TX_WINDOW_PC",FALSE);
        return tp3057_report_blocked(item + "_TX_WINDOW_PC");
    }

    SET_WLD();
    for(data_row=0; data_row<TP3057_FRAME_DATA_ROWS; ++data_row){
        bit_index = data_row / TP3057_STREAM_WINDOW_BIT_CYCLES;
        row_in_bit = data_row % TP3057_STREAM_WINDOW_BIT_CYCLES;
        tp3057_eval_methoda_active_timing(bit_index,row_in_bit,TP3057_METHODA_CLK_REL,TRUE,&timing);
        dx_expect = timing.compare_enable ? (ref_bits[bit_index] ? 'H' : 'L') : 'X';
        if(!tp3057_build_stream_vector(timing.mclk_high,
                                       timing.bclk_high,
                                       timing.fsx_high,
                                       0,
                                       0,
                                       dx_expect,
                                       line_data)){
            SET_CLRWLD();
            return tp3057_report_blocked(item + "_TX_VECTOR_BUILD");
        }
        SET_PC(tp3057_pc_46_data_row(pc,bit_index,row_in_bit));
        LoadData(line_data,TP3057_PATTERN_LINE_BYTES);
    }
    SET_CLRWLD();
    tp3057_show_flag(item + "_TX_STREAM_PATCH",TRUE);
    return TRUE;
}

static BOOL tp3057_bind_rx_stream_chunk(const BYTE* ref_bits,int ref_bit_count,CString item)
{
    BYTE line_data[TP3057_PATTERN_LINE_BYTES];
    TP3057_METHODA_TIMING timing;
    DWORD pc;
    int data_row;
    int bit_index;
    int row_in_bit;

    if(ref_bit_count < TP3057_STREAM_WINDOW_BITS){
        return tp3057_report_missing_reference(item + "_BITS_SHORT");
    }
    if(!tp3057_ensure_pattern_image()){
        tp3057_show_flag(item + "_RX_STREAM_PATCH",FALSE);
        return tp3057_report_blocked(item + "_RX_PATCH_INTERFACE");
    }
    pc = tp3057_start_index_pc(TP3057_START_INDEX_RX_STREAM);
    if(pc == 0x7FFFFF){
        tp3057_show_flag(item + "_RX_WINDOW_PC",FALSE);
        return tp3057_report_blocked(item + "_RX_WINDOW_PC");
    }

    SET_WLD();
    for(data_row=0; data_row<TP3057_FRAME_DATA_ROWS; ++data_row){
        bit_index = data_row / TP3057_STREAM_WINDOW_BIT_CYCLES;
        row_in_bit = data_row % TP3057_STREAM_WINDOW_BIT_CYCLES;
        tp3057_eval_methoda_active_timing(bit_index,row_in_bit,TP3057_METHODA_CLK_REL,FALSE,&timing);
        if(!tp3057_build_stream_vector(timing.mclk_high,
                                       timing.bclk_high,
                                       0,
                                       timing.fsr_high,
                                       ref_bits[bit_index] ? 1 : 0,
                                       'X',
                                       line_data)){
            SET_CLRWLD();
            return tp3057_report_blocked(item + "_RX_VECTOR_BUILD");
        }
        SET_PC(tp3057_pc_47_data_row(pc,bit_index,row_in_bit));
        LoadData(line_data,TP3057_PATTERN_LINE_BYTES);
    }
    SET_CLRWLD();
    tp3057_show_flag(item + "_RX_STREAM_PATCH",TRUE);
    return TRUE;
}

static void tp3057_fill_chunk_word(const BYTE* ref_bits,int ref_bit_count,int start_bit,BYTE* word_bits)
{
    int i;
    for(i=0; i<TP3057_STREAM_WINDOW_BITS; ++i){
        if((start_bit + i) < ref_bit_count){
            word_bits[i] = ref_bits[start_bit + i] ? 1 : 0;
        }
        else{
            word_bits[i] = 0;
        }
    }
}

static int tp3057_limit_chunk_bits(int ref_bit_count,int requested_bits)
{
    int bits;
    bits = ref_bit_count;
    if((requested_bits > 0) && (requested_bits < bits)){
        bits = requested_bits;
    }
    if(bits <= 0){
        return 0;
    }
    bits = ((bits + TP3057_STREAM_WINDOW_BITS - 1) / TP3057_STREAM_WINDOW_BITS) * TP3057_STREAM_WINDOW_BITS;
    return bits;
}

static BOOL tp3057_run_tx_compare_chunked_sequence(const BYTE* ref_bits,int ref_bit_count,int requested_bits,CString item)
{
    BYTE word_bits[TP3057_STREAM_WINDOW_BITS];
    CString chunk_item;
    int bits_to_run;
    int start_bit;

    bits_to_run = tp3057_limit_chunk_bits(ref_bit_count,requested_bits);
    if(bits_to_run <= 0){
        return tp3057_report_missing_reference(item + "_BITS_SHORT");
    }

    // Fail-latch policy is intentionally narrow here: TX compare is the only
    // path that later reads READ_FAIL_DATA(), so clear CHB immediately before
    // a fresh TX compare chunk sequence. This is not a global fail-latch
    // cleanup policy for every pattern run.
    RESET_CHB();

    for(start_bit=0; start_bit<bits_to_run; start_bit+=TP3057_STREAM_WINDOW_BITS){
        tp3057_fill_chunk_word(ref_bits,ref_bit_count,start_bit,word_bits);
        chunk_item.Format("%s_TXW%03d",item,start_bit/TP3057_STREAM_WINDOW_BITS);
        if(!tp3057_bind_tx_compare_chunk(word_bits,TP3057_STREAM_WINDOW_BITS,chunk_item)){
            return FALSE;
        }
        if(!tp3057_run_pattern_auto(46)){
            tp3057_show_flag(chunk_item + "_RUN",FALSE);
            return FALSE;
        }
    }
    return TRUE;
}

static BOOL tp3057_run_rx_stream_chunked_sequence(const BYTE* ref_bits,int ref_bit_count,int requested_bits,CString item)
{
    BYTE word_bits[TP3057_STREAM_WINDOW_BITS];
    CString chunk_item;
    int bits_to_run;
    int start_bit;

    bits_to_run = tp3057_limit_chunk_bits(ref_bit_count,requested_bits);
    if(bits_to_run <= 0){
        return tp3057_report_missing_reference(item + "_BITS_SHORT");
    }

    for(start_bit=0; start_bit<bits_to_run; start_bit+=TP3057_STREAM_WINDOW_BITS){
        tp3057_fill_chunk_word(ref_bits,ref_bit_count,start_bit,word_bits);
        chunk_item.Format("%s_RXW%03d",item,start_bit/TP3057_STREAM_WINDOW_BITS);
        if(!tp3057_bind_rx_stream_chunk(word_bits,TP3057_STREAM_WINDOW_BITS,chunk_item)){
            return FALSE;
        }
        if(!tp3057_run_pattern_auto(47)){
            tp3057_show_flag(chunk_item + "_RUN",FALSE);
            return FALSE;
        }
    }
    return TRUE;
}

static BOOL tp3057_run_pattern_checked(int index)
{
    CString item;
    BOOL pass;

    item.Format("PAT%d_RUN_START",index);
    tp3057_show_flag(item,TRUE);
    if(g_tp3057_pattern_addr != 0){
        pass = RUN_PATTERN(index,g_tp3057_pattern_addr,1,0,0);
    }
    else{
        pass = RUN_PATTERN(TP3057_PATTERN_NAME,index,1,0,0);
    }
    item.Format("PAT%d_RUN_%s",index,pass ? "OK" : "FAIL");
    tp3057_show_flag(item,pass);
    return pass;
}

static BOOL tp3057_pattern_is_dynamic_loop(int index)
{
    switch(index){
    case 22:  // ICC1_MEASURE
    case 23:  // IBB1_MEASURE
    case 31:  // GXA_MEASURE
    case 33:  // GXR_MEASURE
    case 41:  // SFDX_MEASURE
    case 45:  // IMD_MEASURE
    case 48:  // RX_MEASURE_STREAM_LOOP
        return TRUE;
    default:
        return FALSE;
    }
}

static BOOL tp3057_run_pattern_auto(int index,CString item)
{
    BOOL pass;
    pass = tp3057_run_pattern_checked(index);
    if(!pass && tp3057_pattern_is_dynamic_loop(index)){
        // Dynamic LDF/JMP segments can keep the pattern engine looping after
        // a failed RUN_PATTERN. Centralize the cleanup here so call sites do
        // not need to remember which START_INDEX values are dynamic.
        SET_MASKJMP();
        tp3057_show_flag(item + "_DYN_ABORT",FALSE);
    }
    return pass;
}

static BOOL tp3057_run_pattern_auto(int index)
{
    CString item;
    item.Format("PAT%d",index);
    return tp3057_run_pattern_auto(index,item);
}

static void tp3057_show_flag(CString item, BOOL pass)
{
    SHOW_RESULT(item, pass ? 1.0 : 0.0, "flag", 1.0, 0.0);
}

static BOOL tp3057_check_range(CString item,double value,CString unit,double up_limit,double lo_limit)
{
    SHOW_RESULT(item,value,unit,up_limit,lo_limit);
    if(value > up_limit) return FALSE;
    if(value < lo_limit) return FALSE;
    return TRUE;
}

static BOOL tp3057_check_upper(CString item,double value,CString unit,double up_limit)
{
    SHOW_RESULT(item,value,unit,up_limit,No_LoLimit);
    if(value > up_limit) return FALSE;
    return TRUE;
}

static BOOL tp3057_report_missing_reference(CString item)
{
    tp3057_show_flag(item + "_MISSING",FALSE);
    return FALSE;
}

static BOOL tp3057_report_blocked(CString item)
{
    tp3057_show_flag(item + "_BLOCKED",FALSE);
    return FALSE;
}

static BOOL tp3057_validate_ref_blob(const BYTE* ref_data,int ref_count,CString ref_name)
{
    if((ref_data == 0) || (ref_count <= 0)){
        return tp3057_report_missing_reference(ref_name);
    }
    return TRUE;
}

static BOOL tp3057_validate_bit_ref_blob(const BYTE* ref_bits,int ref_bit_count,CString ref_name)
{
    if((ref_bits == 0) || (ref_bit_count <= 0)){
        return tp3057_report_missing_reference(ref_name + "_BITS");
    }
    return TRUE;
}

static BOOL tp3057_check_channel_no_fail(CString item,int channel)
{
    int fail_bank;
    int fail_bit;
    BYTE fail_byte;
    BOOL pass;

    if((channel < 1) || (channel > 64)){
        return tp3057_report_blocked(item + "_CHANNEL_RANGE");
    }

    fail_bank = (channel - 1) / 8;
    fail_bit = (channel - 1) % 8;
    fail_byte = READ_FAIL_DATA(fail_bank);
    SHOW_RESULT(item + "_FAILBYTE",(double)fail_byte,"raw",255.0,0.0);

    pass = ((fail_byte & (1 << fail_bit)) == 0);
    tp3057_show_flag(item,pass);
    return pass;
}

static double tp3057_gain_db(double measured_vrms,double reference_vrms)
{
    if((measured_vrms <= 0.0) || (reference_vrms <= 0.0)){
        return 999.0;
    }
    return 20.0 * log10(measured_vrms / reference_vrms);
}

static BOOL tp3057_require_tx_compare_interface(CString item)
{
#if TP3057_TX_COMPARE_INTERFACE_READY
    return TRUE;
#else
    return tp3057_report_blocked(item + "_TX_COMPARE_INTERFACE");
#endif
}

static BOOL tp3057_require_rx_stream_interface(CString item)
{
#if TP3057_RX_STREAM_INTERFACE_READY
    return TRUE;
#else
    return tp3057_report_blocked(item + "_RX_STREAM_INTERFACE");
#endif
}

static double tp3057_ref_vrms_from_dbm0(double level_dbm0)
{
    return TP3057_GRA_NOMINAL_VRMS * pow(10.0, level_dbm0 / 20.0);
}

static BOOL tp3057_measure_rx_gain_db(CString item,double ref_vrms,double* gain_db)
{
    double vfro_vrms;

    vfro_vrms = AVM_MEASURE(TP3057_AVM_RX_MEASURE_CH,TP3057_AVM_RANGE_FULL_V,V,TP3057_AVM_MEASURE_TDELAY_MS);
    SHOW_RESULT(item + "_VFRO",vfro_vrms,"V",No_UpLimit,0.0);
    if((vfro_vrms <= 0.0) || (ref_vrms <= 0.0)){
        tp3057_show_flag(item,FALSE);
        return FALSE;
    }

    *gain_db = tp3057_gain_db(vfro_vrms,ref_vrms);
    SHOW_RESULT(item,*gain_db,"dB",No_UpLimit,No_LoLimit);
    return TRUE;
}

static BOOL tp3057_run_rx_chunked_window(int ready_index,int measure_index,const BYTE* ref_bits,int ref_bit_count,int requested_bits,CString item)
{
    // v7 note: the `measure_index` parameter is LEGACY and only used as a
    // diagnostic tag. Actual RX measurement ALWAYS runs the shared
    // START_INDEX(48) RX_MEASURE_STREAM_LOOP, regardless of measure_index.
    // Segments 35/37/39/43 in the mdc are also legacy placeholders kept
    // for compatibility; do NOT modify them expecting different behavior.
    CString measure_item;

    if(!tp3057_run_pattern_auto(ready_index)){
        tp3057_show_flag(item + "_READY",FALSE);
        return FALSE;
    }
    if(!tp3057_run_rx_stream_chunked_sequence(ref_bits,ref_bit_count,requested_bits,item)){
        return FALSE;
    }
    if(!tp3057_patch_rx_measure_stream(ref_bits,ref_bit_count,item)){
        return FALSE;
    }
    measure_item.Format("%s_MEASURE_TEMPLATE_%d",item,measure_index);
    tp3057_show_flag(measure_item,TRUE);
    if(!tp3057_run_pattern_auto(TP3057_RX_MEASURE_STREAM_START_INDEX,item + "_MEASURE_RUN")){
        tp3057_show_flag(item + "_MEASURE",FALSE);
        return FALSE;
    }
    return TRUE;
}

static void tp3057_prepare_tx_analog(double source_vrms,double freq_hz)
{
    tp3057_select_avm_tx();
    SET_AS(source_vrms,V,freq_hz,HZ);
    SET_AVM_PATH(TP3057_AVM_LPPASS,TP3057_AVM_BPPASS);
    Delay(TP3057_DELAY_ANALOG_MS);
}

static void tp3057_prepare_rx_observe(void)
{
    tp3057_select_avm_rx();
    SET_AS_DC(0.0,V);
    SET_AVM_PATH(TP3057_AVM_LPPASS,TP3057_AVM_BPPASS);
    Delay(TP3057_DELAY_ANALOG_MS);
}

static BOOL tp3057_prepare_imd_source(void)
{
#if TP3057_IMD_SOURCE_READY
#if TP3057_HAS_AS_PATTERN_API
    if(!LOAD_AS_PATTERN(TP3057_IMD_AS_MODE,TP3057_IMD_AS_BANK,g_tp3057_imd_as_bank0)){
        tp3057_show_flag("IMD_DUALTONE_LOAD",FALSE);
        return FALSE;
    }
    RUN_AS_PATTERN(TP3057_IMD_AS_MODE,TP3057_IMD_AS_BANK,TP3057_IMD_AS_FREQ_DIV,TP3057_IMD_SOURCE_VRMS);
    SET_AVM_PATH(TP3057_AVM_LPPASS,TP3057_AVM_BPPASS);
    Delay(TP3057_DELAY_ANALOG_MS);
    return TRUE;
#else
    return tp3057_report_blocked("IMD_DUALTONE_AS_PATTERN_API");
#endif
#else
    return tp3057_report_missing_reference("IMD_DUALTONE_PATTERN");
#endif
}

static BOOL tp3057_measure_single_tone_distortion_db(int meas_no,CString prefix,double* result_db)
{
    double fundamental_v;
    double residual_v;

    SET_AVM_PATH(TP3057_AVM_LPPASS,TP3057_AVM_BP1K);
    Delay(TP3057_DELAY_ANALOG_MS);
    fundamental_v = AVM_MEASURE(meas_no,TP3057_DIST_MEASURE_RANGE_V,V,TP3057_AVM_MEASURE_TDELAY_MS);
    SHOW_RESULT(prefix + "_FUND",fundamental_v,"V",No_UpLimit,0.0);

    SET_AVM_PATH(TP3057_AVM_LPPASS,TP3057_AVM_BPNOT);
    Delay(TP3057_DELAY_ANALOG_MS);
    residual_v = AVM_MEASURE(meas_no,TP3057_DIST_RESIDUAL_RANGE_V,V,TP3057_AVM_MEASURE_TDELAY_MS);
    SHOW_RESULT(prefix + "_RESID",residual_v,"V",No_UpLimit,0.0);

    SET_AVM_PATH(TP3057_AVM_LPPASS,TP3057_AVM_BPPASS);
    Delay(TP3057_DELAY_ANALOG_MS);

    if(fundamental_v <= 0.0){
        tp3057_show_flag(prefix + "_PATH",FALSE);
        return FALSE;
    }
    if(residual_v <= 0.0){
        tp3057_show_flag(prefix + "_RESID_CLAMPED",TRUE);
        residual_v = 1e-12;
    }

    *result_db = 20.0 * log10(residual_v / fundamental_v);
    SHOW_RESULT(prefix,*result_db,"dB",No_UpLimit,No_LoLimit);
    return TRUE;
}

static void tp3057_remove_dc(double* samples,int count)
{
    int i;
    double mean = 0.0;

    for(i=0; i<count; ++i){
        mean += samples[i];
    }
    mean /= (double)count;

    for(i=0; i<count; ++i){
        samples[i] -= mean;
    }
}

static double tp3057_goertzel_power(const double* samples,int count,double sample_rate,double target_hz)
{
    int i;
    double omega;
    double coeff;
    double q0;
    double q1 = 0.0;
    double q2 = 0.0;

    omega = (2.0 * TP3057_PI * target_hz) / sample_rate;
    coeff = 2.0 * cos(omega);

    for(i=0; i<count; ++i){
        q0 = coeff * q1 - q2 + samples[i];
        q2 = q1;
        q1 = q0;
    }

    return q1*q1 + q2*q2 - coeff*q1*q2;
}

static BOOL tp3057_measure_input_current_pin(CString pin,CString item)
{
    return PMU_MEASURE(pin,TP3057_DELAY_SHORT_MS,item,UA,10.0,-10.0);
}

static BOOL tp3057_measure_con_digit_pin(CString pin,CString item)
{
    PMU_CONDITIONS(FIMV,-0.1,MA,2,V);
    return PMU_MEASURE(pin,20,item,V,-0.1,-1.9);
}

static BOOL tp3057_prepare_encode_reference(void)
{
#if TP3057_FUNC_ENCODE_REF_READY
    if(!tp3057_validate_ref_blob(g_tp3057_func_encode_ref,TP3057_FUNC_ENCODE_REF_COUNT,"FUNC_ENCODE_REF")){
        return FALSE;
    }
    if(!tp3057_validate_bit_ref_blob(g_tp3057_func_encode_ref_bits,TP3057_FUNC_ENCODE_REF_BIT_COUNT,"FUNC_ENCODE_REF")){
        return FALSE;
    }
    return TRUE;
#else
    return tp3057_report_missing_reference("FUNC_ENCODE_REF");
#endif
}

static BOOL tp3057_prepare_decode_reference(void)
{
#if TP3057_FUNC_DECODE_REF_READY
    if(!tp3057_validate_ref_blob(g_tp3057_func_decode_ref,TP3057_FUNC_DECODE_REF_COUNT,"FUNC_DECODE_REF")){
        return FALSE;
    }
    if(!tp3057_validate_bit_ref_blob(g_tp3057_func_decode_ref_bits,TP3057_FUNC_DECODE_REF_BIT_COUNT,"FUNC_DECODE_REF")){
        return FALSE;
    }
    return TRUE;
#else
    return tp3057_report_missing_reference("FUNC_DECODE_REF");
#endif
}

static BOOL tp3057_prepare_gra_reference(void)
{
#if TP3057_GRA_REF_1020HZ_READY
    if(!tp3057_validate_ref_blob(g_tp3057_gra_ref_1020hz,TP3057_GRA_REF_1020HZ_COUNT,"GRA_REF_1020HZ")){
        return FALSE;
    }
    if(!tp3057_validate_bit_ref_blob(g_tp3057_gra_ref_1020hz_bits,TP3057_GRA_REF_1020HZ_BIT_COUNT,"GRA_REF_1020HZ")){
        return FALSE;
    }
    return TRUE;
#else
    return tp3057_report_missing_reference("GRA_REF_1020HZ");
#endif
}

static BOOL tp3057_prepare_grr_reference(const BYTE* ref_data,int ref_count,const BYTE* ref_bits,int ref_bit_count,CString ref_name)
{
#if TP3057_GRR_REF_READY
    if(!tp3057_validate_ref_blob(ref_data,ref_count,ref_name)){
        return FALSE;
    }
    if(!tp3057_validate_bit_ref_blob(ref_bits,ref_bit_count,ref_name)){
        return FALSE;
    }
    return TRUE;
#else
    return tp3057_report_missing_reference(ref_name);
#endif
}

static BOOL tp3057_prepare_grrl_reference(const BYTE* ref_data,int ref_count,const BYTE* ref_bits,int ref_bit_count,CString ref_name)
{
#if TP3057_GRRL_REF_READY
    if(!tp3057_validate_ref_blob(ref_data,ref_count,ref_name)){
        return FALSE;
    }
    if(!tp3057_validate_bit_ref_blob(ref_bits,ref_bit_count,ref_name)){
        return FALSE;
    }
    return TRUE;
#else
    return tp3057_report_missing_reference(ref_name);
#endif
}

static BOOL tp3057_prepare_sfdr_reference(void)
{
#if TP3057_SFDR_REF_READY
    if(!tp3057_validate_ref_blob(g_tp3057_sfdr_ref,TP3057_SFDR_REF_COUNT,"SFDR_REF")){
        return FALSE;
    }
    if(!tp3057_validate_bit_ref_blob(g_tp3057_sfdr_ref_bits,TP3057_SFDR_REF_BIT_COUNT,"SFDR_REF")){
        return FALSE;
    }
    return TRUE;
#else
    return tp3057_report_missing_reference("SFDR_REF");
#endif
}

static BOOL tp3057_run_sfdx_algo(double* result_db)
{
    return tp3057_measure_single_tone_distortion_db(TP3057_AVM_TX_MEASURE_CH,"SFDX_PATH",result_db);
}

static BOOL tp3057_run_sfdr_algo(double* result_db)
{
    return tp3057_measure_single_tone_distortion_db(TP3057_AVM_RX_MEASURE_CH,"SFDR_PATH",result_db);
}

static BOOL tp3057_run_imd_algo(double* result_db)
{
    double waveform[TP3057_IMD_DVM_POINTS];
    double sample_rate;
    double tone1_power;
    double tone2_power;
    double imd_low_power;
    double imd_high_power;
    double fundamental_power;
    double worst_imd_power;

    sample_rate = 50000000.0 / (double)TP3057_IMD_DVM_FREQ_DIV;
    MAT_DVM_MEASURE(TP3057_IMD_DVM_CHANNEL,
                    TP3057_IMD_DVM_RANGE_V,
                    V,
                    10,
                    TP3057_IMD_DVM_POINTS,
                    TP3057_IMD_DVM_FREQ_DIV,
                    waveform);

    tp3057_remove_dc(waveform,TP3057_IMD_DVM_POINTS);

    tone1_power = tp3057_goertzel_power(waveform,TP3057_IMD_DVM_POINTS,sample_rate,TP3057_IMD_TONE1_HZ);
    tone2_power = tp3057_goertzel_power(waveform,TP3057_IMD_DVM_POINTS,sample_rate,TP3057_IMD_TONE2_HZ);
    imd_low_power = tp3057_goertzel_power(waveform,TP3057_IMD_DVM_POINTS,sample_rate,(2.0*TP3057_IMD_TONE1_HZ)-TP3057_IMD_TONE2_HZ);
    imd_high_power = tp3057_goertzel_power(waveform,TP3057_IMD_DVM_POINTS,sample_rate,(2.0*TP3057_IMD_TONE2_HZ)-TP3057_IMD_TONE1_HZ);

    SHOW_RESULT("IMD_F1_PWR",tone1_power,"V2",No_UpLimit,0.0);
    SHOW_RESULT("IMD_F2_PWR",tone2_power,"V2",No_UpLimit,0.0);
    SHOW_RESULT("IMD_2F1_F2_PWR",imd_low_power,"V2",No_UpLimit,0.0);
    SHOW_RESULT("IMD_2F2_F1_PWR",imd_high_power,"V2",No_UpLimit,0.0);

    fundamental_power = tone1_power;
    if(tone2_power > fundamental_power){
        fundamental_power = tone2_power;
    }
    worst_imd_power = imd_low_power;
    if(imd_high_power > worst_imd_power){
        worst_imd_power = imd_high_power;
    }

    if(fundamental_power <= 0.0){
        tp3057_show_flag("IMD_PATH",FALSE);
        return FALSE;
    }
    if(worst_imd_power <= 0.0){
        tp3057_show_flag("IMD_RESID_CLAMPED",TRUE);
        worst_imd_power = 1e-24;
    }

    *result_db = 10.0 * log10(worst_imd_power / fundamental_power);
    SHOW_RESULT("IMD_PATH",*result_db,"dB",No_UpLimit,No_LoLimit);
    return TRUE;
}

static void tp3057_run_optional_imd(void)
{
    double distortion_db;

    tp3057_begin_powered_block();
    if(!tp3057_prepare_imd_source()){
        tp3057_power_off();
        tp3057_show_flag("IMD_OPTIONAL",FALSE);
        return;
    }
    if(!tp3057_run_pattern_auto(44)){
        tp3057_power_off();
        tp3057_show_flag("IMD_OPTIONAL",FALSE);
        return;
    }
    if(!tp3057_run_pattern_auto(45,"IMD_MEASURE")){
        tp3057_power_off();
        tp3057_show_flag("IMD_OPTIONAL",FALSE);
        return;
    }
    if(!tp3057_run_imd_algo(&distortion_db)){
        SET_MASKJMP();
        tp3057_power_off();
        tp3057_show_flag("IMD_OPTIONAL",FALSE);
        return;
    }
    SET_MASKJMP();
    (void)tp3057_check_upper("IMD_OPTIONAL",distortion_db,"dB",-41.0);
    tp3057_power_off();
}

void PASCAL tp3057()
{
    double value;
    double ibb_abs;
    double gxa_db;
    double gxr_ref_db;
    double gxr_delta_db;
    double gra_db;
    double grr_ref_db;
    double grr_delta_db;
    double grrl_ref_db;
    double grrl_gain_db;
    double grrl_delta_db;
    double distortion_db;

    // Default production behavior caches LOAD_PATTERN. Enable this switch
    // during offline debug if the MDV may be regenerated between tp3057() calls.
#if TP3057_FORCE_PATTERN_RELOAD_EACH_RUN
    g_tp3057_pattern_addr = 0;
#endif
    if(!tp3057_report_methoda_timing_selfcheck()){
        tp3057_show_flag("METHA_LAYOUT_BLOCKED",FALSE);
        tp3057_power_off();
        // Program/graph consistency is blocked. Reject explicitly through
        // legacy bin 34, which maps to the final Bin 15 reject bucket.
        tp3057_bin(TP3057_LEGACY_BIN_METHODA_LAYOUT_BLOCKED);
        return;
    }

    // connectivity test (required)
    tp3057_begin_unpowered_block();
    if(!tp3057_run_pattern_auto(9)) { tp3057_power_off(); tp3057_bin(1); return; }
    tp3057_show_flag("CON_DIG_START",TRUE);
    if(!tp3057_measure_con_digit_pin("1","CON_DIG_1")) { tp3057_show_flag("CON_DIG_FAIL_PATH",TRUE); tp3057_power_off(); tp3057_bin(1); return; }
    if(!tp3057_measure_con_digit_pin("2","CON_DIG_2")) { tp3057_show_flag("CON_DIG_FAIL_PATH",TRUE); tp3057_power_off(); tp3057_bin(1); return; }
    if(!tp3057_measure_con_digit_pin("3","CON_DIG_3")) { tp3057_show_flag("CON_DIG_FAIL_PATH",TRUE); tp3057_power_off(); tp3057_bin(1); return; }
    if(!tp3057_measure_con_digit_pin("4","CON_DIG_4")) { tp3057_show_flag("CON_DIG_FAIL_PATH",TRUE); tp3057_power_off(); tp3057_bin(1); return; }
    if(!tp3057_measure_con_digit_pin("5","CON_DIG_5")) { tp3057_show_flag("CON_DIG_FAIL_PATH",TRUE); tp3057_power_off(); tp3057_bin(1); return; }
    if(!tp3057_measure_con_digit_pin("6","CON_DIG_6")) { tp3057_show_flag("CON_DIG_FAIL_PATH",TRUE); tp3057_power_off(); tp3057_bin(1); return; }
    if(!tp3057_measure_con_digit_pin("7","CON_DIG_7")) { tp3057_show_flag("CON_DIG_FAIL_PATH",TRUE); tp3057_power_off(); tp3057_bin(1); return; }
    if(!tp3057_measure_con_digit_pin("8","CON_DIG_8")) { tp3057_show_flag("CON_DIG_FAIL_PATH",TRUE); tp3057_power_off(); tp3057_bin(1); return; }
    if(!tp3057_measure_con_digit_pin("9","CON_DIG_9")) { tp3057_show_flag("CON_DIG_FAIL_PATH",TRUE); tp3057_power_off(); tp3057_bin(1); return; }
    tp3057_show_flag("CON_DIG_AFTER_MEASURE",TRUE);
    tp3057_show_flag("CON_DIG_PASS_PATH",TRUE);

    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(10)) { tp3057_power_off(); tp3057_bin(2); return; }
    SET_AS_DC(0.1,V);
    SET_AVM_PATH(TP3057_AVM_LPPASS,TP3057_AVM_BPPASS);
    Delay(TP3057_DELAY_ANALOG_MS);
    value = DVM_MEASURE(2,5,V,50);
    if(!tp3057_check_range("CON_ANA_GSX",value,"V",5.3,-5.3)) { tp3057_power_off(); tp3057_bin(2); return; }
    value = DVM_MEASURE(1,5,V,50);
    if(!tp3057_check_range("CON_ANA_VFRO",value,"V",5.3,-5.3)) { tp3057_power_off(); tp3057_bin(2); return; }

    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(11)) { tp3057_power_off(); tp3057_bin(3); return; }
    if(!tp3057_run_pattern_auto(18)) { tp3057_power_off(); tp3057_bin(3); return; }
    value = DPS_MEASURE(TP3057_VCC_DPS,R20MA,TP3057_DELAY_SHORT_MS);
    if(!tp3057_check_range("CON_PWR_VCC",value,"mA",20.0,0.1)) { tp3057_power_off(); tp3057_bin(3); return; }
    value = fabs(DPS_MEASURE(TP3057_VBB_DPS,R20MA,TP3057_DELAY_SHORT_MS));
    if(!tp3057_check_range("CON_PWR_VBB",value,"mA",20.0,0.1)) { tp3057_power_off(); tp3057_bin(3); return; }

    // digital interface (required)
    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(12)) { tp3057_power_off(); tp3057_bin(4); return; }
    PMU_CONDITIONS(FVMI,0.6,V,50,UA);
    if(!tp3057_measure_input_current_pin(TP3057_PIN_MCLKR_PDN,"IIL_MCLKR_PDN")) { tp3057_power_off(); tp3057_bin(4); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_BCLKR_CLKSEL,"IIL_BCLKR_CLKSEL")) { tp3057_power_off(); tp3057_bin(5); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_MCLKX,"IIL_MCLKX")) { tp3057_power_off(); tp3057_bin(6); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_BCLKX,"IIL_BCLKX")) { tp3057_power_off(); tp3057_bin(7); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_FSX,"IIL_FSX")) { tp3057_power_off(); tp3057_bin(8); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_FSR,"IIL_FSR")) { tp3057_power_off(); tp3057_bin(9); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_DR,"IIL_DR")) { tp3057_power_off(); tp3057_bin(10); return; }

    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(13)) { tp3057_power_off(); tp3057_bin(11); return; }
    PMU_CONDITIONS(FVMI,2.2,V,50,UA);
    if(!tp3057_measure_input_current_pin(TP3057_PIN_MCLKR_PDN,"IIH_MCLKR_PDN")) { tp3057_power_off(); tp3057_bin(11); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_BCLKR_CLKSEL,"IIH_BCLKR_CLKSEL")) { tp3057_power_off(); tp3057_bin(12); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_MCLKX,"IIH_MCLKX")) { tp3057_power_off(); tp3057_bin(13); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_BCLKX,"IIH_BCLKX")) { tp3057_power_off(); tp3057_bin(14); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_FSX,"IIH_FSX")) { tp3057_power_off(); tp3057_bin(15); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_FSR,"IIH_FSR")) { tp3057_power_off(); tp3057_bin(16); return; }
    if(!tp3057_measure_input_current_pin(TP3057_PIN_DR,"IIH_DR")) { tp3057_power_off(); tp3057_bin(17); return; }

    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(14)) { tp3057_power_off(); tp3057_bin(18); return; }
    PMU_CONDITIONS(FVMI,5.0,V,50,UA);
    if(!PMU_MEASURE(TP3057_PIN_DX,TP3057_DELAY_SHORT_MS,"IOZH_DX",UA,10.0,-10.0)) { tp3057_power_off(); tp3057_bin(18); return; }

    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(15)) { tp3057_power_off(); tp3057_bin(19); return; }
    PMU_CONDITIONS(FVMI,0.0,V,50,UA);
    if(!PMU_MEASURE(TP3057_PIN_DX,TP3057_DELAY_SHORT_MS,"IOZL_DX",UA,10.0,-10.0)) { tp3057_power_off(); tp3057_bin(19); return; }

    // power dissipation (required)
    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(16)) { tp3057_power_off(); tp3057_bin(20); return; }
    if(!tp3057_run_pattern_auto(5)) { tp3057_power_off(); tp3057_bin(20); return; }
    if(!tp3057_run_pattern_auto(20)) { tp3057_power_off(); tp3057_bin(20); return; }
    if(!DPS_MEASURE(TP3057_VCC_DPS,R20MA,TP3057_DELAY_POWER_CURRENT_MS,"ICC0",MA,1.5,No_LoLimit)) { tp3057_power_off(); tp3057_bin(20); return; }

    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(17)) { tp3057_power_off(); tp3057_bin(21); return; }
    if(!tp3057_run_pattern_auto(5)) { tp3057_power_off(); tp3057_bin(21); return; }
    if(!tp3057_run_pattern_auto(21)) { tp3057_power_off(); tp3057_bin(21); return; }
    ibb_abs = fabs(DPS_MEASURE(TP3057_VBB_DPS,R2MA,TP3057_DELAY_POWER_CURRENT_MS));
    if(!tp3057_check_upper("IBB0",ibb_abs,"mA",0.3)) { tp3057_power_off(); tp3057_bin(21); return; }

    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(18)) { tp3057_power_off(); tp3057_bin(22); return; }
    if(!tp3057_run_pattern_auto(22,"ICC1_MEASURE")) { tp3057_power_off(); tp3057_bin(22); return; }
    if(!DPS_MEASURE(TP3057_VCC_DPS,R20MA,TP3057_DELAY_POWER_CURRENT_MS,"ICC1",MA,9.0,No_LoLimit)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(22); return; }
    SET_MASKJMP();

    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(19)) { tp3057_power_off(); tp3057_bin(23); return; }
    if(!tp3057_run_pattern_auto(23,"IBB1_MEASURE")) { tp3057_power_off(); tp3057_bin(23); return; }
    ibb_abs = fabs(DPS_MEASURE(TP3057_VBB_DPS,R20MA,TP3057_DELAY_POWER_CURRENT_MS));
    if(!tp3057_check_upper("IBB1",ibb_abs,"mA",9.0)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(23); return; }
    SET_MASKJMP();

    // amplitude response: TX path (required)
    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(30)) { tp3057_power_off(); tp3057_bin(24); return; }
    tp3057_prepare_tx_analog(TP3057_GXA_NOMINAL_VRMS,1020.0);
    if(!tp3057_run_pattern_auto(31,"GXA_MEASURE")) { tp3057_power_off(); tp3057_bin(24); return; }
    value = AVM_MEASURE(TP3057_AVM_TX_MEASURE_CH,TP3057_AVM_RANGE_FULL_V,V,TP3057_AVM_MEASURE_TDELAY_MS);
    gxa_db = tp3057_gain_db(value,TP3057_GXA_NOMINAL_VRMS);
    if(!tp3057_check_range("GXA",gxa_db,"dB",0.15,-0.15)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(24); return; }
    SET_MASKJMP();

    tp3057_begin_powered_block();
    if(!tp3057_run_pattern_auto(32)) { tp3057_power_off(); tp3057_bin(25); return; }
    tp3057_prepare_tx_analog(TP3057_GXA_NOMINAL_VRMS,1020.0);
    if(!tp3057_run_pattern_auto(33,"GXR_1020HZ_MEASURE")) { tp3057_power_off(); tp3057_bin(25); return; }
    gxr_ref_db = tp3057_gain_db(AVM_MEASURE(TP3057_AVM_TX_MEASURE_CH,TP3057_AVM_RANGE_FULL_V,V,TP3057_AVM_MEASURE_TDELAY_MS),TP3057_GXA_NOMINAL_VRMS);
    SET_MASKJMP();

    tp3057_prepare_tx_analog(TP3057_GXA_NOMINAL_VRMS,300.0);
    if(!tp3057_run_pattern_auto(33,"GXR_300HZ_MEASURE")) { tp3057_power_off(); tp3057_bin(25); return; }
    gxr_delta_db = tp3057_gain_db(AVM_MEASURE(TP3057_AVM_TX_MEASURE_CH,TP3057_AVM_RANGE_FULL_V,V,TP3057_AVM_MEASURE_TDELAY_MS),TP3057_GXA_NOMINAL_VRMS) - gxr_ref_db;
    if(!tp3057_check_range("GXR_300HZ",gxr_delta_db,"dB",0.15,-0.15)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(25); return; }
    SET_MASKJMP();

    tp3057_prepare_tx_analog(TP3057_GXA_NOMINAL_VRMS,3000.0);
    if(!tp3057_run_pattern_auto(33,"GXR_3000HZ_MEASURE")) { tp3057_power_off(); tp3057_bin(25); return; }
    gxr_delta_db = tp3057_gain_db(AVM_MEASURE(TP3057_AVM_TX_MEASURE_CH,TP3057_AVM_RANGE_FULL_V,V,TP3057_AVM_MEASURE_TDELAY_MS),TP3057_GXA_NOMINAL_VRMS) - gxr_ref_db;
    if(!tp3057_check_range("GXR_3000HZ",gxr_delta_db,"dB",0.15,-0.15)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(25); return; }
    SET_MASKJMP();

    // distortion: TX path (required)
    tp3057_begin_powered_block();
    tp3057_prepare_tx_analog(TP3057_GXA_NOMINAL_VRMS,1020.0);
    if(!tp3057_run_pattern_auto(40)) { tp3057_power_off(); tp3057_bin(26); return; }
    if(!tp3057_run_pattern_auto(41,"SFDX_MEASURE")) { tp3057_power_off(); tp3057_bin(26); return; }
    if(!tp3057_run_sfdx_algo(&distortion_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(26); return; }
    if(!tp3057_check_upper("SFDX",distortion_db,"dB",-46.0)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(26); return; }
    SET_MASKJMP();

    // optional IMD: does not affect required-item binning
    tp3057_run_optional_imd();

    // function test (required, encode uses full chunked DX compare length)
    tp3057_begin_powered_block();
    if(!tp3057_prepare_encode_reference()) { tp3057_power_off(); tp3057_bin(27); return; }
    if(!tp3057_require_tx_compare_interface("FUNC_ENCODE")) { tp3057_power_off(); tp3057_bin(27); return; }
    tp3057_prepare_tx_analog(TP3057_GXA_NOMINAL_VRMS,1020.0);
    if(!tp3057_run_pattern_auto(24)) { tp3057_power_off(); tp3057_bin(27); return; }
    if(!tp3057_run_pattern_auto(25)) { tp3057_power_off(); tp3057_bin(27); return; }
    if(!tp3057_run_tx_compare_chunked_sequence(g_tp3057_func_encode_ref_bits,TP3057_FUNC_ENCODE_REF_BIT_COUNT,TP3057_TX_COMPARE_MEASURE_BITS,"FUNC_ENCODE")) { tp3057_power_off(); tp3057_bin(27); return; }
    if(!tp3057_check_channel_no_fail("FUNC_ENCODE_DX_COMPARE",8)) { tp3057_power_off(); tp3057_bin(27); return; }

    tp3057_begin_powered_block();
    tp3057_prepare_rx_observe();
    if(!tp3057_prepare_decode_reference()) { tp3057_power_off(); tp3057_bin(28); return; }
    if(!tp3057_require_rx_stream_interface("FUNC_DECODE")) { tp3057_power_off(); tp3057_bin(28); return; }
#if TP3057_RX_STREAM_INTERFACE_READY
    if(!tp3057_run_rx_chunked_window(26,27,g_tp3057_func_decode_ref_bits,TP3057_FUNC_DECODE_REF_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"FUNC_DECODE")) { tp3057_power_off(); tp3057_bin(28); return; }
    if(!tp3057_measure_rx_gain_db("FUNC_DECODE_GAIN",TP3057_GRA_NOMINAL_VRMS,&gra_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(28); return; }
    if(!tp3057_check_range("FUNC_DECODE_GAIN_SANITY",gra_db,"dB",0.5,-0.5)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(28); return; }
    SET_MASKJMP();
#endif

    // Legacy Bin 29 is intentionally unused. The independent TX/RX functional
    // paths are already covered by the encode and decode items above.

    // amplitude response: RX path (required, using chunked framed RX stream windows)
    tp3057_begin_powered_block();
    tp3057_prepare_rx_observe();
    if(!tp3057_prepare_gra_reference()) { tp3057_power_off(); tp3057_bin(30); return; }
    if(!tp3057_require_rx_stream_interface("GRA")) { tp3057_power_off(); tp3057_bin(30); return; }
#if TP3057_RX_STREAM_INTERFACE_READY
    if(!tp3057_run_rx_chunked_window(34,35,g_tp3057_gra_ref_1020hz_bits,TP3057_GRA_REF_1020HZ_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRA")) { tp3057_power_off(); tp3057_bin(30); return; }
    if(!tp3057_measure_rx_gain_db("GRA",TP3057_GRA_NOMINAL_VRMS,&gra_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(30); return; }
    if(!tp3057_check_range("GRA_LIMIT",gra_db,"dB",0.15,-0.15)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(30); return; }
    SET_MASKJMP();
#endif

    tp3057_begin_powered_block();
    tp3057_prepare_rx_observe();
    if(!tp3057_prepare_grr_reference(g_tp3057_grr_ref_1020hz,TP3057_GRR_REF_1020HZ_COUNT,g_tp3057_grr_ref_1020hz_bits,TP3057_GRR_REF_1020HZ_BIT_COUNT,"GRR_REF_1020HZ")) { tp3057_power_off(); tp3057_bin(31); return; }
    if(!tp3057_prepare_grr_reference(g_tp3057_grr_ref_300hz,TP3057_GRR_REF_300HZ_COUNT,g_tp3057_grr_ref_300hz_bits,TP3057_GRR_REF_300HZ_BIT_COUNT,"GRR_REF_300HZ")) { tp3057_power_off(); tp3057_bin(31); return; }
    if(!tp3057_prepare_grr_reference(g_tp3057_grr_ref_3000hz,TP3057_GRR_REF_3000HZ_COUNT,g_tp3057_grr_ref_3000hz_bits,TP3057_GRR_REF_3000HZ_BIT_COUNT,"GRR_REF_3000HZ")) { tp3057_power_off(); tp3057_bin(31); return; }
    if(!tp3057_require_rx_stream_interface("GRR")) { tp3057_power_off(); tp3057_bin(31); return; }
#if TP3057_RX_STREAM_INTERFACE_READY
    if(!tp3057_run_rx_chunked_window(36,37,g_tp3057_grr_ref_1020hz_bits,TP3057_GRR_REF_1020HZ_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRR_REF_1020HZ")) { tp3057_power_off(); tp3057_bin(31); return; }
    if(!tp3057_measure_rx_gain_db("GRR_1020HZ",TP3057_GRA_NOMINAL_VRMS,&grr_ref_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(31); return; }
    SET_MASKJMP();
    if(!tp3057_run_rx_chunked_window(36,37,g_tp3057_grr_ref_300hz_bits,TP3057_GRR_REF_300HZ_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRR_REF_300HZ")) { tp3057_power_off(); tp3057_bin(31); return; }
    grr_delta_db = 0.0;
    if(!tp3057_measure_rx_gain_db("GRR_300HZ",TP3057_GRA_NOMINAL_VRMS,&grr_delta_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(31); return; }
    grr_delta_db -= grr_ref_db;
    if(!tp3057_check_range("GRR_300HZ_DELTA",grr_delta_db,"dB",0.15,-0.15)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(31); return; }
    SET_MASKJMP();
    if(!tp3057_run_rx_chunked_window(36,37,g_tp3057_grr_ref_3000hz_bits,TP3057_GRR_REF_3000HZ_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRR_REF_3000HZ")) { tp3057_power_off(); tp3057_bin(31); return; }
    grr_delta_db = 0.0;
    if(!tp3057_measure_rx_gain_db("GRR_3000HZ",TP3057_GRA_NOMINAL_VRMS,&grr_delta_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(31); return; }
    grr_delta_db -= grr_ref_db;
    if(!tp3057_check_range("GRR_3000HZ_DELTA",grr_delta_db,"dB",0.15,-0.15)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(31); return; }
    SET_MASKJMP();
#endif

    tp3057_begin_powered_block();
    tp3057_prepare_rx_observe();
    if(!tp3057_prepare_grrl_reference(g_tp3057_grrl_ref_m40dbm0,TP3057_GRRL_REF_M40DBM0_COUNT,g_tp3057_grrl_ref_m40dbm0_bits,TP3057_GRRL_REF_M40DBM0_BIT_COUNT,"GRRL_REF_M40DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_prepare_grrl_reference(g_tp3057_grrl_ref_m30dbm0,TP3057_GRRL_REF_M30DBM0_COUNT,g_tp3057_grrl_ref_m30dbm0_bits,TP3057_GRRL_REF_M30DBM0_BIT_COUNT,"GRRL_REF_M30DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_prepare_grrl_reference(g_tp3057_grrl_ref_m20dbm0,TP3057_GRRL_REF_M20DBM0_COUNT,g_tp3057_grrl_ref_m20dbm0_bits,TP3057_GRRL_REF_M20DBM0_BIT_COUNT,"GRRL_REF_M20DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_prepare_grrl_reference(g_tp3057_grrl_ref_m10dbm0,TP3057_GRRL_REF_M10DBM0_COUNT,g_tp3057_grrl_ref_m10dbm0_bits,TP3057_GRRL_REF_M10DBM0_BIT_COUNT,"GRRL_REF_M10DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_prepare_grrl_reference(g_tp3057_grrl_ref_0dbm0,TP3057_GRRL_REF_0DBM0_COUNT,g_tp3057_grrl_ref_0dbm0_bits,TP3057_GRRL_REF_0DBM0_BIT_COUNT,"GRRL_REF_0DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_prepare_grrl_reference(g_tp3057_grrl_ref_p3dbm0,TP3057_GRRL_REF_P3DBM0_COUNT,g_tp3057_grrl_ref_p3dbm0_bits,TP3057_GRRL_REF_P3DBM0_BIT_COUNT,"GRRL_REF_P3DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_require_rx_stream_interface("GRRL")) { tp3057_power_off(); tp3057_bin(32); return; }
#if TP3057_RX_STREAM_INTERFACE_READY
    if(!tp3057_run_rx_chunked_window(38,39,g_tp3057_grrl_ref_0dbm0_bits,TP3057_GRRL_REF_0DBM0_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRRL_REF_0DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_measure_rx_gain_db("GRRL_0DBM0",tp3057_ref_vrms_from_dbm0(g_tp3057_grrl_levels_dbm0[4]),&grrl_ref_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    SET_MASKJMP();
    if(!tp3057_run_rx_chunked_window(38,39,g_tp3057_grrl_ref_m40dbm0_bits,TP3057_GRRL_REF_M40DBM0_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRRL_REF_M40DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_measure_rx_gain_db("GRRL_M40DBM0",tp3057_ref_vrms_from_dbm0(g_tp3057_grrl_levels_dbm0[0]),&grrl_gain_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    grrl_delta_db = grrl_gain_db - grrl_ref_db;
    if(!tp3057_check_range("GRRL_M40DBM0_DELTA",grrl_delta_db,"dB",0.2,-0.2)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    SET_MASKJMP();
    if(!tp3057_run_rx_chunked_window(38,39,g_tp3057_grrl_ref_m30dbm0_bits,TP3057_GRRL_REF_M30DBM0_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRRL_REF_M30DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_measure_rx_gain_db("GRRL_M30DBM0",tp3057_ref_vrms_from_dbm0(g_tp3057_grrl_levels_dbm0[1]),&grrl_gain_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    grrl_delta_db = grrl_gain_db - grrl_ref_db;
    if(!tp3057_check_range("GRRL_M30DBM0_DELTA",grrl_delta_db,"dB",0.2,-0.2)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    SET_MASKJMP();
    if(!tp3057_run_rx_chunked_window(38,39,g_tp3057_grrl_ref_m20dbm0_bits,TP3057_GRRL_REF_M20DBM0_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRRL_REF_M20DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_measure_rx_gain_db("GRRL_M20DBM0",tp3057_ref_vrms_from_dbm0(g_tp3057_grrl_levels_dbm0[2]),&grrl_gain_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    grrl_delta_db = grrl_gain_db - grrl_ref_db;
    if(!tp3057_check_range("GRRL_M20DBM0_DELTA",grrl_delta_db,"dB",0.2,-0.2)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    SET_MASKJMP();
    if(!tp3057_run_rx_chunked_window(38,39,g_tp3057_grrl_ref_m10dbm0_bits,TP3057_GRRL_REF_M10DBM0_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRRL_REF_M10DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_measure_rx_gain_db("GRRL_M10DBM0",tp3057_ref_vrms_from_dbm0(g_tp3057_grrl_levels_dbm0[3]),&grrl_gain_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    grrl_delta_db = grrl_gain_db - grrl_ref_db;
    if(!tp3057_check_range("GRRL_M10DBM0_DELTA",grrl_delta_db,"dB",0.2,-0.2)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    SET_MASKJMP();
    if(!tp3057_run_rx_chunked_window(38,39,g_tp3057_grrl_ref_p3dbm0_bits,TP3057_GRRL_REF_P3DBM0_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"GRRL_REF_P3DBM0")) { tp3057_power_off(); tp3057_bin(32); return; }
    if(!tp3057_measure_rx_gain_db("GRRL_P3DBM0",tp3057_ref_vrms_from_dbm0(g_tp3057_grrl_levels_dbm0[5]),&grrl_gain_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    grrl_delta_db = grrl_gain_db - grrl_ref_db;
    if(!tp3057_check_range("GRRL_P3DBM0_DELTA",grrl_delta_db,"dB",0.2,-0.2)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(32); return; }
    SET_MASKJMP();
#endif

    // distortion: RX path (required, using chunked framed RX stream windows)
    tp3057_begin_powered_block();
    tp3057_prepare_rx_observe();
    if(!tp3057_prepare_sfdr_reference()) { tp3057_power_off(); tp3057_bin(33); return; }
    if(!tp3057_require_rx_stream_interface("SFDR")) { tp3057_power_off(); tp3057_bin(33); return; }
#if TP3057_RX_STREAM_INTERFACE_READY
    if(!tp3057_run_rx_chunked_window(42,43,g_tp3057_sfdr_ref_bits,TP3057_SFDR_REF_BIT_COUNT,TP3057_RX_STREAM_MEASURE_BITS,"SFDR")) { tp3057_power_off(); tp3057_bin(33); return; }
    if(!tp3057_run_sfdr_algo(&distortion_db)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(33); return; }
    if(!tp3057_check_upper("SFDR",distortion_db,"dB",-46.0)) { SET_MASKJMP(); tp3057_power_off(); tp3057_bin(33); return; }
    SET_MASKJMP();
#endif

    tp3057_power_off();
    tp3057_bin(0);
}
