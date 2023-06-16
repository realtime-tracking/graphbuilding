if { $argc == 1} {
    set MODULE [lindex $argv 0]
} else {
    put "No module name found. Exiting."
    exit 1
}

set_part "xcvu160-flgb2104-2-e"

#Set Paths
set SRC_PATH ../src/hdl
set CONST_PATH ../src/constr
set CHECKPOINT_PATH ../build

#Load external source files
set file_list [glob -nocomplain -directory $SRC_PATH *.sv]
if {[llength $file_list] != 0} {
    read_verilog $file_list
} else {
    puts "No external Verilog Design Sources found."
}

#Load source files
set file_list [glob -nocomplain -directory $SRC_PATH *.v]
if {[llength $file_list] != 0} {
    read_verilog $file_list
} else {
    puts "No Verilog Design Sources found."
}

#Load OOC constraints
read_xdc -mode out_of_context $CONST_PATH/constrOocModule.xdc

#Start OOC synthesis
synth_design -mode out_of_context -flatten_hierarchy rebuilt -top $MODULE

#Save result
write_checkpoint -force $CHECKPOINT_PATH/$MODULE.check

report_timing_summary -delay_type min_max -report_unconstrained -check_timing_verbose -max_paths 10 -input_pins -routable_nets -file $CHECKPOINT_PATH/${MODULE}OocTimingReport.txt
report_utilization -file $CHECKPOINT_PATH/${MODULE}OocUtilizationReport.txt
