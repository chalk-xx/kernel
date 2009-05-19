#!/usr/bin/perl

package Sling::AuthUtil;

=head1 NAME

AuthUtil - useful utility functions for general Auth functionality.

=head1 ABSTRACT

Utility library providing useful utility functions for general Auth functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
#}}}

#{{{sub basic_login_setup

=pod

=head2 basic_login_setup

Returns a textual representation of the request needed to log the user in to
the system via a basic auth based login.

=cut

sub basic_login_setup {
    my ( $baseURL ) = @_;
    return "get $baseURL/system/sling/login?sling:authRequestLogin=1";
}
#}}}

#{{{sub basic_login_eval

=pod

=head2 basic_login_eval

Verify whether the log in attempt for the user to the system was successful.

=cut

sub basic_login_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
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
    $username = Sling::URL::urlencode( $username );
    $password = Sling::URL::urlencode( $password );
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

1;

