#!/usr/bin/perl

package Sling::Util;

=head1 NAME

Util - useful utility functions for general Rest functionality.

=head1 ABSTRACT

Utility library providing useful utility functions for general Rest functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use HTTP::Request::Common qw(GET POST);
use vars qw(@ISA @EXPORT);
use LWP::UserAgent ();
use Fcntl ':flock';
#}}}

@ISA = qw(Exporter);

require Exporter;

@EXPORT = qw(VERSION_MESSAGE);

#{{{sub VERSION_MESSAGE
sub VERSION_MESSAGE {
    my ( $out, $optPackage, $optVersion, $switches ) = @_;
    print "$0 version sling calling $optPackage (version $optVersion)\n\n";
}
#}}}

#{{{sub dateTime

=pod

=head2 dateTime

Returns a current date time string.

=cut

sub dateTime {
    my @months = qw(Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec);
    my @weekDays = qw(Sun Mon Tue Wed Thu Fri Sat Sun);
    (my $second, my $minute, my $hour, my $dayOfMonth,
     my $month, my $yearOffset, my $dayOfWeek, my $dayOfYear, my $daylightSavings) = localtime();
    $second = "0$second" if $second < 10;
    $second = "0$minute" if $minute < 10;
    my $year = 1900 + $yearOffset;
    return "$weekDays[$dayOfWeek] $months[$month] $dayOfMonth $hour:$minute:$second";
}
#}}}

#{{{sub get_user_agent

=pod

=head2 get_user_agent

Returns a user agent object, set up to follow redirects on POST and with a
suitable cookie container.

=cut

sub get_user_agent {
    my $lwpUserAgent = LWP::UserAgent->new( keep_alive=>1 );
    push @{ $lwpUserAgent->requests_redirectable }, 'POST';
    $lwpUserAgent->cookie_jar( { file => "/tmp/RestCookies$$.txt" });
    return \$lwpUserAgent;
}
#}}}

#{{{sub help_header
sub help_header {
    my ( $script, $switches ) = @_;
    print "Usage: $script [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]\n\n";
    print "The following single-character options are accepted: $switches\n";
    print "Those with a ':' after require an argument to be supplied:\n\n";
}
#}}}

#{{{sub help_footer
sub help_footer {
    my ( $script ) = @_;
    print "\nOptions may be merged together. -- stops processing of options.\n";
    print "Space is not required between options and their arguments.\n";
    print "For more details run: perldoc -F $script\n";
}
#}}}

#{{{sub print_file_lock

=pod

=head2 print_file_lock

Prints out a specified message to a specified file with locking in an attempt
to prevent competing threads or forks from stepping on each others toes when
writing to the file.

=cut

sub print_file_lock {
    my ( $message, $file ) = @_;
    if ( open( FILE, ">>$file" ) ) {
        flock( FILE, LOCK_EX );
        print FILE $message . "\n";
        flock( FILE, LOCK_UN );
        close( FILE );
    }
    else {
        die "Could not open file: $file";
    }
    return 1;
}
#}}}

#{{{sub print_lock

=pod

=head2 print_lock

Prints out a specified message with locking in an attempt to prevent competing
threads or forks from stepping on each others toes when printing to stdout.

=cut

sub print_lock {
    my ( $message ) = @_;
    if ( open( LOCK, ">>/tmp/RestLock$$.txt" ) ) {
        flock( LOCK, LOCK_EX );
        print $message . "\n";
        flock( LOCK, LOCK_UN );
        close( LOCK );
        unlink( "/tmp/RestLock$$.txt" );
    }
    else {
        die "Could not open lock file: /tmp/RestLock$$.txt";
    }
    return 1;
}
#}}}

#{{{sub string_to_request

=pod

=head2 string_to_request

Function taking a string and converting to a GET or POST HTTP request.

=cut

sub string_to_request {
    my ( $string ) = @_;
    my ( $action, $target, @reqVariables ) = split( ' ', $string );
    if ( $action =~ /^post$/ ) {
        my $variables = join( " ", @reqVariables );
        my $postVariables;
        no strict;
        eval $variables;
        use strict;
	my $request = POST ( "$target", $postVariables );
	return $request;
    }
    elsif ( $action =~ /^data$/ ) {
        # multi-part form upload
        my $variables = join( " ", @reqVariables );
        my $postVariables;
        no strict;
        eval $variables;
        use strict;
	my $request = POST ( "$target", $postVariables, 'Content_Type' => 'form-data' );
	return $request;
    }
    elsif ( $action =~ /^fileupload$/ ) {
        # multi-part form upload with the file name and file specified
        my $filename = shift( @reqVariables );
        my $file = shift( @reqVariables );
        my $variables = join( " ", @reqVariables );
        my $postVariables;
        no strict;
        eval $variables;
        use strict;
	push ( @{ $postVariables }, $filename => [ "$file" ] );
	my $request = POST ( "$target", $postVariables, 'Content_Type' => 'form-data' );
	return $request;
    }
    else {
        my $request = GET "$target";
	return $request;
    }
}
#}}}

#{{{sub urlencode

=pod

=head2 urlencode

Function to encode a string so it is suitable for use in urls.

=cut

sub urlencode {
    my ( $value ) = @_;
    $value =~ s/([^a-zA-Z_0-9 ])/"%" . uc(sprintf "%lx" , unpack("C", $1))/eg;
    $value =~ tr/ /+/;
    return ($value);
}
#}}}

1;
