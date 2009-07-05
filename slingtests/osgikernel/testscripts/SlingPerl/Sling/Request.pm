#!/usr/bin/perl

package Sling::Request;

=head1 NAME

Request - useful utility functions for general Request functionality.

=head1 ABSTRACT

Utility library providing useful utility functions for general Request functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use HTTP::Request::Common qw(GET POST);
use MIME::Base64;
use Sling::Print;
#}}}

#{{{sub string_to_request

=pod

=head2 string_to_request

Function taking a string and converting to a GET or POST HTTP request.

=cut

sub string_to_request {
    my ( $string, $auth, $verbose, $log ) = @_;
    die "No string defined to turn into request!" unless defined $string;
    my $lwp = $$auth->{ 'LWP' };
    die "No reference to an lwp user agent supplied!" unless defined $lwp;
    my ( $action, $target, @reqVariables ) = split( ' ', $string );
    my $request;
    if ( $action =~ /^post$/ ) {
        my $variables = join( " ", @reqVariables );
        my $postVariables;
        no strict;
        my $success = eval $variables;
        use strict;
        if ( ! defined $success ) {
	    die "Error \"$@\" parsing post variables: \"$variables\"";
	}
	$request = POST ( "$target", $postVariables );
    }
    elsif ( $action =~ /^data$/ ) {
        # multi-part form upload
        my $variables = join( " ", @reqVariables );
        my $postVariables;
        no strict;
        my $success = eval $variables;
        use strict;
        if ( ! defined $success ) {
	    die "Error \"$@\" parsing post variables: \"$variables\"";
	}
	$request = POST ( "$target", $postVariables, 'Content_Type' => 'form-data' );
    }
    elsif ( $action =~ /^fileupload$/ ) {
        # multi-part form upload with the file name and file specified
        my $filename = shift( @reqVariables );
        my $file = shift( @reqVariables );
        my $variables = join( " ", @reqVariables );
        my $postVariables;
        no strict;
        my $success = eval $variables;
        use strict;
        if ( ! defined $success ) {
	    die "Error \"$@\" parsing post variables: \"$variables\"";
	}
	push ( @{ $postVariables }, $filename => [ "$file" ] );
	$request = POST ( "$target", $postVariables, 'Content_Type' => 'form-data' );
    }
    else {
        $request = GET "$target";
    }
<<<<<<< HEAD:slingtests/osgikernel/testscripts/SlingPerl/Sling/Request.pm
    if ( $$auth->{ 'Type' } =~ /^basic$/ ) {
        my $username = $$auth->{ 'Username' };
	my $password = $$auth->{ 'Password' };
=======
    if ( defined $lwp ) {
print "FOO\n";
        my $realm = Sling::URL::url_to_realm( $target );
        my ( $username, $password ) = $$lwp->credentials( $realm, 'Sling (Development)' );
print "BAR $username, $password $realm\n";
>>>>>>> aaron/master:slingtests/osgikernel/testscripts/SlingPerl/Sling/Request.pm
        if ( defined $username && defined $password ) {
	    # Always add an Authorization header to deal with application not
	    # properly requesting authentication to be sent:
            my $encoded = "Basic " . encode_base64("$username:$password");
            $request->header( 'Authorization' => $encoded );
        }
    }
    if ( $verbose >= 3 ) {
        Sling::Print::print_with_lock( "**** String representation of compiled request:\n" . $request->as_string, $log );
    }
        print $request->as_string;
    return $request;
}
#}}}

#{{{sub request

=pod

=head2 request

Function to actually issue an HTTP request given a suitable string
representation of the request and an object which references a suitable LWP
object.

=cut

sub request {
    my ( $object, $string ) = @_;
    die "No string defined to turn into request!" unless defined $string;
    die "No reference to a suitable object supplied!" unless defined $object;
    my $auth = $$object->{ 'Auth' };
    die "Object does not reference a suitable auth object" unless defined $auth;
    my $verbose = $$object->{ 'Verbose' };
    my $log = $$object->{ 'Log' };
    my $lwp = $$auth->{ 'LWP' };
    my $res = $$lwp->request( string_to_request( $string, $auth, $verbose, $log ) );
    return \$res;
}
#}}}

1;
