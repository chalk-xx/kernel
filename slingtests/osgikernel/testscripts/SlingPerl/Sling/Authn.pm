#!/usr/bin/perl

package Sling::Authn;

=head1 NAME

Authn - useful utility functions for general Authn functionality.

=head1 ABSTRACT

Utility library providing useful utility functions for general Authn functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::AuthnUtil;
use Sling::Print;
use Sling::Request;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a User Agent.

=cut

sub new {
    my ( $class, $url, $lwpUserAgent ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $auth = { BaseURL => "$url",
                 LWP => $lwpUserAgent,
		 Message => "",
		 Response => \$response };
    bless( $auth, $class );
    return $auth;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $user, $message, $response ) = @_;
    $user->{ 'Message' } = $message;
    $user->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub basic_login
sub basic_login {
    my ( $auth, $log ) = @_;
    my $res = ${ $auth->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::AuthnUtil::basic_login_setup( $auth->{ 'BaseURL' } ), $auth->{ 'LWP' } ) );
    my $success = Sling::AuthnUtil::basic_login_eval( \$res );
    my $message = "Basic auth log in ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $auth->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub form_login
sub form_login {
    my ( $auth, $username, $password, $log ) = @_;
    my $res = ${ $auth->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::AuthnUtil::form_login_setup( $auth->{ 'BaseURL' }, $username, $password ), $auth->{ 'LWP' } ) );
    my $success = Sling::AuthnUtil::form_login_eval( \$res );
    my $message = "Form log in as user \"$username\" ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $auth->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub form_logout
sub form_logout {
    my ( $auth, $log ) = @_;
    my $res = ${ $auth->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::AuthnUtil::form_logout_setup( $auth->{ 'BaseURL' } ), $auth->{ 'LWP' } ) );
    my $success = Sling::AuthnUtil::form_logout_eval( \$res );
    my $message = "Form log out ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $auth->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

1;
