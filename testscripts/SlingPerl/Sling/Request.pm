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
use HTTP::Request::Common qw(DELETE GET POST PUT);
use MIME::Base64;
use Sling::Print;
#}}}

#{{{sub string_to_request

=pod

=head2 string_to_request

Function taking a string and converting to a GET or POST HTTP request.

=cut

sub string_to_request {
    my ( $string, $authn, $verbose, $log ) = @_;
    die "No string defined to turn into request!" unless defined $string;
    my $lwp = $$authn->{ 'LWP' };
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
    elsif ( $action =~ /^put$/ ) {
        $request = PUT "$target";
    }
    elsif ( $action =~ /^delete$/ ) {
        $request = DELETE "$target";
    }
    else {
        $request = GET "$target";
    }
    if ( $$authn->{ 'Type' } =~ /^basic$/ ) {
        my $username = $$authn->{ 'Username' };
	my $password = $$authn->{ 'Password' };
        if ( defined $username && defined $password ) {
	    # Always add an Authorization header to deal with application not
	    # properly requesting authentication to be sent:
            my $encoded = "Basic " . encode_base64("$username:$password");
            $request->header( 'Authorization' => $encoded );
        }
    }
    if ( $verbose >= 2 ) {
        Sling::Print::print_with_lock( "**** String representation of compiled request:\n" . $request->as_string, $log );
    }
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
    my $authn = $$object->{ 'Authn' };
    die "Object does not reference a suitable auth object" unless defined $authn;
    my $verbose = $$object->{ 'Verbose' };
    my $log = $$object->{ 'Log' };
    my $lwp = $$authn->{ 'LWP' };
    my $res = $$lwp->request( string_to_request( $string, $authn, $verbose, $log ) );
    return \$res;
}
#}}}

1;
