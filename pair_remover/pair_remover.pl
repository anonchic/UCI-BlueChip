#!/usr/bin/perl

# Basically finds something like the architechture block, or the comb block.
# pass in the name you want, and it'll return the start index (where "architecture" or "comb" is), 
# the begin index (where "begin" is), and the end index (where "end" is)

sub locate_block {
    my ($start, @content) = @_;

    my $start_index = -1;
    my $begin_index = -1;
    my $end_index = -1;

    my $index = 0;

    while ($index <= $#content) {
	if ($content[$index] =~ m/$start/i) {
	    #print("Found start at index $index\n");
	    $start_index = $index;
	    last;
	}
	$index++;
    }

    # Locate the correct end
    my $depth = 1;
    $index = $start_index + 1;
    
    while ($index <= $#content) {
	if ($content[$index] =~ m/procedure/i or $content[$index] =~ m/function/i) {
	    #print("Increasing depth at $index: $content[$index]\n");
	    $depth++;
	}
	
	if ($content[$index] =~ m/^\s*end;/i) {
	    #print("Decreasing depth at $index: $content[$index]\n");
	    $depth--;
	}
	if ($depth == 1 and $content[$index] =~ m/begin/i) {
	    #print("Located begin at $index: $content[$index]\n");
	    last;
	}
	$index++;
    }
    
    $begin_index = $index;
    
    $index = $begin_index;
    $depth = 1;
    while ($index <= $#content) {
	if ($content[$index] =~ m/procedure/i or $content[$index] =~ m/function/i) {
	    #print("Increasing depth at $index: $content[$index]\n");
	    $depth++;
	}
	if ($content[$index] =~ m/^\s*end;/i) {
	    #print("Decreasing depth at $index: $content[$index]\n");
	    $depth--;
	}
	if ($depth == 0) {
	    #print("Found end index at $index\n");
	    $end_index = $index;
	    last;
	}
	$index++;
    }
    
    $end_index = $index;
    
    return ($start_index, $begin_index, $end_index);
}

# This function locates the BC added line that must be modified after we rename the signal.

sub locate_bc {
    ($target, @content) = @_;

    $safe_target = regex_safe($target);

    my $bc_name = bc_name($target);
    $bc_name .= '_';

    for ($index = 0; $index <= $#content; $index++) {
	if ($content[$index] =~ m/^\s*$bc_name.*:?<?=\s*$safe_target\s*;/i) {
	    return $index;
	}
    }

    return -1;
    
}

sub locate_declaration {
    ($target, @content) = @_;
    
    for ($index = 0; $index <= $#content; $index++) {
	if ($content[$index] =~ m/^\s*signal\s*$target\s*:\s*(.*);/i) {
	    return ('signal', $index, $1);
	}
	if ($content[$index] =~ m/^\s*variable\s*$target\s*:\s*(.*);/i) {
	    return ('variable', $index, $1);
	}
    }
    
    return -1;   
}



sub new_target {
    $target = shift;
    
    $target =~ s/\./_/g;
    $target =~ s/__/_/g;
    $target =~ s/\s//g;
    $target =~ s/\(//g;
    $target =~ s/\)//g;

    $target .= '_removed';
    
    return $target;
}

sub regex_safe {
    $value = shift;

    $value =~ s/\(/\\\(/g;
    $value =~ s/\)/\\\)/g;
    $value =~ s/\s+/\\s\*/g;
    $value =~ s/\./\\./g;    

    return $value;
}

sub bc_name {
    $target = shift;
    $target = uc $target;
    $target =~ s/\./_/g;
    $target =~ s/__/_/g;
    
    return $target;
}


#Begin processing

$argc = $#ARGV+1;
if ($argc != 3) {
    print "Usage: ./pair_remover.pl inputvhdl pairfile outputvhdl\n";
    die();
}

$input_vhdl_name = $ARGV[0];
$input_pair_name = $ARGV[1];
$output_vhdl_name = $ARGV[2];

open(FILE, $input_vhdl_name) or die ("Unable to open input vhdl file");
@input_vhdl = <FILE>;
close(FILE);

open(FILE, $input_pair_name) or die ("Unable to open pair file");
@pairs = <FILE>;
close(FILE);

@output_vhdl = @input_vhdl;

my %targets;

# replaces will be an array of all the replacements that are needed
my @replaces;

# bc_replaces_done will be a hash of all the BC lines already done
my %bc_replaces_done;

# Now, check the pairs
foreach $pair (@pairs) {
    ($target, $driver, $delay) = split(/&/, $pair);

    if ($target =~ m/([A-Za-z]+)\/(.*)/) {
	$proc = $1;
	$target = $2;
    } 	else  {
	$proc = 'architecture';
    }

    $target_nospace = $target;
    $target_nospace =~ s/\s//g;

    if ($targets{"$proc"}{"$target_nospace"} > 0) {
	$targets{"$proc"}{"$target_nospace"}++;
    } else {
	$targets{"$proc"}{"$target_nospace"} = 1;

	my $start_index = -1;
	my $begin_index = -1;
	my $end_index = -1;
    
	($start_index, $begin_index, $end_index) = locate_block($proc, @input_vhdl);
	
	$regex_safe_target = regex_safe($target);
	
	for ($index = $begin_index; $index < $end_index; $index++) {
	    if ($input_vhdl[$index] =~ m/$regex_safe_target\s*<?:?=\s*/i) {
		push @replaces, {"proc" => $proc, "target" => $target, "line" => $index, "driver" => $driver, "delay" => $delay};
	    }
	}
    }
}

# Locate the definitions that must be created

my @definitions;

foreach $replace (@replaces) {
    $index = locate_bc($replace->{target}, @input_vhdl);
    if ($index > 0) {
	$line = $input_vhdl[$index];
	$line =~ m/\s*(.*)[:<]=\s*(.*);/;
	my $bc_equiv = $1;	
	
	# find the declaration
	($type, $index, $decl) = locate_declaration($bc_equiv, @input_vhdl);
	$line = $input_vhdl[$index];
	
	push @definitions, {"proc" => $replace->{proc}, "type" => $type, "decl" => $decl, "name" => new_target($replace->{target})};
    }
}


#Now, create the output vhdl
my @output = @input_vhdl;

foreach $replace (@replaces) {
    print("Replacing $replace->{target} on line $replace->{line}\n");
    
    $safe_target = regex_safe($replace->{target});
    $new_target = new_target($replace->{target});
    $replacements = @output[$replace->{line}] =~ s/$safe_target(\s*<?:?=\s*)/$new_target $1 /ig;
    print("$replacements replacement(s) made.\n");
    
    $bc_index = locate_bc($replace->{target}, @input_vhdl);
    if ($bc_index >= 0 and not $bc_replaces_done->{$bc_index}) {
	print("A BC replacement must be done on line $bc_index\n");
	$name = bc_name($replace->{target});
	$replacements = @output[$bc_index] =~ s/(<?:?=)\s*$safe_target/$1 $new_target/g;
	print("$replacements replacement(s) made.\n");
	$bc_replaces_done->{$bc_index} = 1;
    }
}

# now insert the declarations

foreach $definition (@definitions) {
    ($start_index, $begin_index, $end_index) = locate_block($definition->{proc}, @input_vhdl);
    
    if ($definition->{proc} == "comb") {
	$definition->{type} = "variable";
    }
    
    splice(@output, $begin_index-1, 0, ($definition->{type} . " " . $definition->{name} . " : " .  $definition->{decl} . "\n"));    
}


open (FILE, ">$output_vhdl_name");
foreach $line (@output) {
    print FILE $line;
}
close FILE;
print "done\n";
