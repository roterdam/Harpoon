#!/usr/bin/gnuplot -persist
#
#    
#    	G N U P L O T
#    	Linux version 3.7
#    	patchlevel 1
#    	last modified Fri Oct 22 18:00:00 BST 1999
#    
#    	Copyright(C) 1986 - 1993, 1998, 1999
#    	Thomas Williams, Colin Kelley and many others
#    
#    	Type `help` to access the on-line reference manual
#    	The gnuplot FAQ is available from
#    	<http://www.ucc.ie/gnuplot/gnuplot-faq.html>
#    
#    	Send comments and requests for help to <info-gnuplot@dartmouth.edu>
#    	Send bugs, suggestions and mods to <bug-gnuplot@dartmouth.edu>
#    
set terminal postscript eps enhanced monochrome dashed defaultplex "Helvetica" 14
set output 'uvsimCounterRuntime.eps'
set noclip points
set clip one
set noclip two
set bar 1.000000
set border 3 lt -1 lw 1.000
set xdata
set ydata
set zdata
set x2data
set y2data
set boxwidth
set dummy x,y
set format x "%g"
set format y "%g"
set format x2 "%g"
set format y2 "%g"
set format z "%g"
set angles radians
set nogrid
set key title ""
set key left top Left reverse box linetype -2 linewidth 1.000 samplen 4 spacing 1 width 0
set nolabel
set noarrow
set nolinestyle
set nologscale
#set logscale x 10
#set logscale y 10
set offsets 0, 0, 0, 0
set pointsize 1
set encoding default
set nopolar
set noparametric
set view 60, 30, 1, 1
set samples 100, 100
set isosamples 10, 10
set surface
set nocontour
set clabel '%8.3g'
set mapping cartesian
set nohidden3d
set cntrparam order 4
set cntrparam linear
set cntrparam levels auto 5
set cntrparam points 5
set size ratio 0 0.6,0.35
set origin 0,0
set data style points
set function style lines
set xzeroaxis lt -2 lw 1.000
set x2zeroaxis lt -2 lw 1.000
set yzeroaxis lt -2 lw 1.000
set y2zeroaxis lt -2 lw 1.000
set tics in
set ticslevel 0.5
set ticscale 1 0.5
set mxtics default
set mytics default
set mx2tics default
set my2tics default
set xtics border nomirror norotate autofreq 
#set xtics (2,4,8,16)
set ytics border nomirror norotate autofreq 
# set ytics (0,120,"" 200, "" 400, "" 600, 680)
set ztics border nomirror norotate autofreq 
set nox2tics
set noy2tics
set title "" 0.000000,0.000000  ""
set timestamp "" bottom norotate 0.000000,0.000000  ""
set rrange [ * : * ] noreverse nowriteback  # (currently [-0.00000:10.0000] )
set trange [ * : * ] noreverse nowriteback  # (currently [-5.00000:5.00000] )
set urange [ * : * ] noreverse nowriteback  # (currently [-5.00000:5.00000] )
set vrange [ * : * ] noreverse nowriteback  # (currently [-5.00000:5.00000] )
set xlabel "Number of processors" 0.000000,0.000000  ""
set x2label "" 0.000000,0.000000  ""
set timefmt "%d/%m/%y\n%H:%M"
set xrange [ * : * ] noreverse nowriteback  # (currently [-10.0000:10.0000] )
set x2range [ * : * ] noreverse nowriteback  # (currently [-10.0000:10.0000] )
set ylabel "Avg. cycles per iteration" 0.000000,0.000000  ""
set y2label "" 0.000000,0.000000  ""
set yrange [ * : * ] noreverse nowriteback  # (currently [-10.0000:10.0000] )
set y2range [ * : * ] noreverse nowriteback  # (currently [-10.0000:10.0000] )
set zlabel "" 0.000000,0.000000  ""
set zrange [ * : * ] noreverse nowriteback  # (currently [-10.0000:10.0000] )
set zero 1e-08
set lmargin -1
set bmargin -1
set rmargin -1
set tmargin -1
set locale "C"
plot "uvsimCounterRuntime.txt" using 1:2 title "locks" with linespoints , "uvsimCounterRuntime.txt" using 1:3 title "transactions" with linespoints
#    EOF