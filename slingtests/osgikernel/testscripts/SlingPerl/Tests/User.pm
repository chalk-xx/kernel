#!/usr/bin/perl

package Tests::User;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::User;
use Test::More tests => 4;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the user object.

=cut

sub run_regression_test {
    my ( $url, $lwpUserAgent, $log, $verbose ) = @_;
    # test user name:
    my $test_user = "testing_user_$$";
    # test user pass:
    my $test_pass = "pass";
    # test properties:
    my @test_properties;
    # Sling user object:
    my $user = new Sling::User( $url, $lwpUserAgent, $verbose );

    # Run tests:
    ok( defined $user,
        "Sling User Object successfully created." );
    ok( $user->add( $test_user, $test_pass, \@test_properties, $log ),
        "User \"$test_user\" added successfully." );
    ok( ! $user->add( "g-$test_user", $test_pass, \@test_properties, $log ),
        "User \"g-$test_user\" creation denied." );
    ok( $user->delete( $test_user, $log ),
        "User \"$test_user\" deleted successfully." );
}
#}}}

1;
