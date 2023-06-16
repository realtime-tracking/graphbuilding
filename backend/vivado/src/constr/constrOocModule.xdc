create_clock -period 3.930 -name clk_in -waveform {0.000 1.965} [get_ports {clock}]
set_property HD.CLK_SRC BUFGCTRL_X0Y0 [get_ports {clock}]