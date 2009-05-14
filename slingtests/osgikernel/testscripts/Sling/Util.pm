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
    # Optional parameters if auth is desired:
    my ( $url, $username, $password, $type ) = @_;
    my $lwpUserAgent = LWP::UserAgent->new( keep_alive=>1 );
    push @{ $lwpUserAgent->requests_redirectable }, 'POST';
    $lwpUserAgent->cookie_jar( { file => "/tmp/RestCookies$$.txt" });
    # Apply basic authentication to the user agent if url, username and
    # password are supplied:
    if ( defined $url && defined $username && defined $password ) {
        my $loginType = ( defined $type ? $type : "basic" );
	if ( $loginType =~ /^basic$/ ) {
            my $realm = $url;
            if ( $realm !~ /:[0-9]+$/ ) {
                # No port specified yet, need to add one:
                $realm = ( $realm =~ /^http:/ ? "$realm:80" : "$realm:443" );
            }
            # Strip the protocol for the realm:
            $realm =~ s#https?://(.*)#$1#;
            $lwpUserAgent->credentials( $realm, 'Sling (Development)',
	                                $username => $password, );
        }
	elsif ( $loginType =~ /^form$/ ) {
	    form_login( $url, \$lwpUserAgent, $username, $password );
	}
	else {
	    die "Unsupported login type: \"$loginType\""; 
	}
    }
    return \$lwpUserAgent;
}
#}}}

#{{{sub help_header
sub help_header {
    my ( $script ) = @_;
    print "Usage: $script [-OPTIONS [-MORE_OPTIONS]] [--] [PROGRAM_ARG1 ...]\n\n";
    print "The following options are accepted:\n\n";
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

#{{{sub form_login
sub form_login {
    my ( $url, $lwpUserAgent, $username, $password ) = @_;
    my $res = ${ $lwpUserAgent }->request( string_to_request(
        form_login_setup( $url, $username, $password ) ) );
    my $success = form_login_eval( \$res );
    my $message = "Log in as user \"$username\" ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    print $message . "\n";
    return $success;
}
#}}}

#{{{sub form_login_setup

=pod

=head2 form_login_setup

Returns a textual representation of the request needed to log the user in to
the system via a form based login.

=cut

sub form_login_setup {
    my ( $baseURL, $username, $password ) = @_;
    die "No username supplied to attempt logging in with!" unless defined $username;
    die "No password supplied to attempt logging in with for user name: $username!" unless defined $password;
    $username = urlencode( $username );
    $password = urlencode( $password );
    my $type = "FORM";
    my $postVariables = "\$postVariables = ['sakaiauth:un','$username','sakaiauth:pw','$password','sakaiauth:login','1']";
    return "post $baseURL/system/sling/formlogin $postVariables";
}
#}}}

#{{{sub form_login_eval

=pod

=head2 form_login_eval

Verify whether the log in attempt for the user to the system was successful.

=cut

sub form_login_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
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
