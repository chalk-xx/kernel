#!/usr/bin/perl

package Tests::Authn;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Authn;
use Sling::User;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the authn object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test user name:
    my $test_user = "testing_user_$$";
    # test user pass:
    my $test_pass = "pass";
    # test properties:
    my @test_properties;
    # Sling user1 object:
    my $user = new Sling::User( $authn, $verbose, $log );

    my $super_user = $$authn->{ 'Username' };
    my $super_pass = $$authn->{ 'Password' };

    # Run tests:
    ok( defined $user,
        "Authn Test: Sling User Object successfully created." );

    # Add two users:
    ok( $user->add( $test_user . "_1", $test_pass, \@test_properties ),
        "Authn Test: User \"$test_user\" added successfully." );
    ok( $user->exists( $test_user . "_1" ),
        "Authn Test: User \"$test_user\" exists." );
    ok( $user->add( $test_user . "_2", $test_pass, \@test_properties ),
        "Authn Test: User \"$test_user\" added successfully." );
    ok( $user->exists( $test_user . "_2" ),
        "Authn Test: User \"$test_user\" exists." );

    ok( $$authn->switch_user( $test_user . "_1", $test_pass, "basic", 1 ),
        "Authn Test: Successfully switched to user: \"" . $test_user . "_1\" with basic auth" );
    ok( $$authn->switch_user( $test_user . "_2", $test_pass, "form", 1 ),
        "Authn Test: Successfully switched to user: \"" . $test_user . "_2\" with form auth" );
    ok( $$authn->switch_user( $super_user, $super_pass, "basic", 1 ),
        "Authn Test: Successfully switched back to user: \"" . $super_user . "_1\" with basic auth" );

    ok( $user->delete( $test_user . "_1" ),
        "Authn Test: User \"" . $test_user . "_1\" deleted successfully." );
    ok( ! $user->exists( $test_user . "_1" ),
        "Authn Test: User \"" . $test_user . "_1\" should no longer exist." );
    ok( $user->delete( $test_user . "_2" ),
        "Authn Test: User \"" . $test_user . "_2\" deleted successfully." );
    ok( ! $user->exists( $test_user . "_2" ),
        "Authn Test: User \"" . $test_user . "_2\" should no longer exist." );
}
#}}}

1;
