#!/usr/bin/perl

package Tests::Connection;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Connection;
use Sling::User;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the connection object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test users:
    my $test_user1 = "connection_test_user_1_$$";
    my $test_user2 = "connection_test_user_2_$$";
    my $test_user3 = "connection_test_user_3_$$";
    my $test_user4 = "connection_test_user_4_$$";
    my $test_user5 = "connection_test_user_5_$$";
    my $test_user6 = "connection_test_user_6_$$";
    my $test_user7 = "connection_test_user_7_$$";
    # test user pass:
    my $test_pass = "pass";
    # test properties:
    my @test_properties;
    # Sling connection object:
    my $connection = new Sling::Connection( $authn, $verbose, $log );
    # Sling user object:
    my $user = new Sling::User( $authn, $verbose, $log );

    my $super_user = $$authn->{ 'Username' };
    my $super_pass = $$authn->{ 'Password' };

    # Run tests:
    ok( defined $user,
        "Connection Test: Sling User Object successfully created." );
    ok( defined $connection,
        "Connection Test: Sling Connection Object successfully created." );

    ok( $user->add( $test_user1, $test_pass, \@test_properties ),
        "Connection Test: User \"$test_user1\" added successfully." );
    ok( $user->exists( $test_user1 ),
        "Connection Test: User \"$test_user1\" exists." );
    ok( $user->add( $test_user2, $test_pass, \@test_properties ),
        "Connection Test: User \"$test_user2\" added successfully." );
    ok( $user->exists( $test_user2 ),
        "Connection Test: User \"$test_user2\" exists." );
    ok( $user->add( $test_user3, $test_pass, \@test_properties ),
        "Connection Test: User \"$test_user3\" added successfully." );
    ok( $user->exists( $test_user3 ),
        "Connection Test: User \"$test_user3\" exists." );
    ok( $user->add( $test_user4, $test_pass, \@test_properties ),
        "Connection Test: User \"$test_user4\" added successfully." );
    ok( $user->exists( $test_user4 ),
        "Connection Test: User \"$test_user4\" exists." );
    ok( $user->add( $test_user5, $test_pass, \@test_properties ),
        "Connection Test: User \"$test_user5\" added successfully." );
    ok( $user->exists( $test_user5 ),
        "Connection Test: User \"$test_user5\" exists." );
    ok( $user->add( $test_user6, $test_pass, \@test_properties ),
        "Connection Test: User \"$test_user6\" added successfully." );
    ok( $user->exists( $test_user6 ),
        "Connection Test: User \"$test_user6\" exists." );
    ok( $user->add( $test_user7, $test_pass, \@test_properties ),
        "Connection Test: User \"$test_user7\" added successfully." );
    ok( $user->exists( $test_user7 ),
        "Connection Test: User \"$test_user7\" exists." );

    # Issue invitations:
    ok( $$authn->switch_user( $test_user1, $test_pass ),
        "Connection Test: Successfully switched to user: \"" . $test_user1 . "\" with basic auth" );
    my @types = ( "classmate" );
    ok( $connection->invite( $test_user2, \@types ),
        "Connection Test: User \"$test_user1\" successfully invited: \"$test_user2\"" );
    @types = ( "supervisor" );
    ok( $connection->invite( $test_user3, \@types ),
        "Connection Test: User \"$test_user1\" successfully invited: \"$test_user3\"" );
    @types = ( "lecturer" );
    ok( $connection->invite( $test_user4, \@types ),
        "Connection Test: User \"$test_user1\" successfully invited: \"$test_user4\"" );
    @types = ( "student" );
    ok( $connection->invite( $test_user5, \@types ),
        "Connection Test: User \"$test_user1\" successfully invited: \"$test_user5\"" );
    @types = ( "colleague" );
    ok( $connection->invite( $test_user6, \@types ),
        "Connection Test: User \"$test_user1\" successfully invited: \"$test_user6\"" );
    @types = ( "friend" );
    ok( $connection->invite( $test_user7, \@types ),
        "Connection Test: User \"$test_user1\" successfully invited: \"$test_user7\"" );

    # Respond to invitations:
    ok( $$authn->switch_user( $test_user2, $test_pass ),
        "Connection Test: Successfully switched to user: \"$test_user2\" with basic auth" );
    ok( $connection->accept( $test_user1 ),
        "Connection Test: User \"$test_user2\" successfully accepted invitation from: \"$test_user1\"" );
    ok( $$authn->switch_user( $test_user3, $test_pass ),
        "Connection Test: Successfully switched to user: \"$test_user3\" with basic auth" );
    ok( $connection->block( $test_user1 ),
        "Connection Test: User \"$test_user3\" successfully blocked invitation from: \"$test_user1\"" );
    ok( $$authn->switch_user( $test_user4, $test_pass ),
        "Connection Test: Successfully switched to user: \"$test_user4\" with basic auth" );
    ok( $connection->ignore( $test_user1 ),
        "Connection Test: User \"$test_user4\" successfully ignored invitation from: \"$test_user1\"" );
    ok( $$authn->switch_user( $test_user5, $test_pass ),
        "Connection Test: Successfully switched to user: \"$test_user5\" with basic auth" );
    ok( $connection->reject( $test_user1 ),
        "Connection Test: User \"$test_user5\" successfully rejected invitation from: \"$test_user1\"" );
    ok( $$authn->switch_user( $test_user6, $test_pass ),
        "Connection Test: Successfully switched to user: \"$test_user6\" with basic auth" );
    ok( $connection->cancel( $test_user1 ),
        "Connection Test: User \"$test_user6\" successfully cancelled pending invitation from: \"$test_user1\"" );
    ok( $$authn->switch_user( $test_user1, $test_pass ),
        "Connection Test: Successfully switched to user: \"$test_user1\" with basic auth" );
    ok( $connection->remove( $test_user2 ),
        "Connection Test: User \"$test_user1\" successfully removed connection to user \"$test_user2\"" );
    ok( $connection->remove( $test_user3 ),
        "Connection Test: User \"$test_user1\" successfully removed connection to user \"$test_user3\"" );
    ok( $connection->remove( $test_user4 ),
        "Connection Test: User \"$test_user1\" successfully removed connection to user \"$test_user4\"" );
    ok( $connection->remove( $test_user5 ),
        "Connection Test: User \"$test_user1\" successfully removed connection to user \"$test_user5\"" );
    ok( ! $connection->remove( $test_user6 ),
        "Connection Test: User \"$test_user1\" found connection to user \"$test_user6\" already removed" );
    ok( $connection->cancel( $test_user7 ),
        "Connection Test: User \"$test_user1\" successfully cancelled connection to user \"$test_user7\"" );

    ok( $$authn->switch_user( $super_user, $super_pass ),
        "Connection Test: Successfully switched to user: \"$super_user\" with basic auth" );
    ok( $user->delete( $test_user1 ),
        "Connection Test: User \"$test_user1\" deleted successfully." );
    ok( ! $user->exists( $test_user1 ),
        "Connection Test: User \"$test_user1\" should no longer exist." );
    ok( $user->delete( $test_user2 ),
        "Connection Test: User \"$test_user2\" deleted successfully." );
    ok( ! $user->exists( $test_user2 ),
        "Connection Test: User \"$test_user2\" should no longer exist." );
    ok( $user->delete( $test_user3 ),
        "Connection Test: User \"$test_user3\" deleted successfully." );
    ok( ! $user->exists( $test_user3 ),
        "Connection Test: User \"$test_user3\" should no longer exist." );
    ok( $user->delete( $test_user4 ),
        "Connection Test: User \"$test_user4\" deleted successfully." );
    ok( ! $user->exists( $test_user4 ),
        "Connection Test: User \"$test_user4\" should no longer exist." );
    ok( $user->delete( $test_user5 ),
        "Connection Test: User \"$test_user5\" deleted successfully." );
    ok( ! $user->exists( $test_user5 ),
        "Connection Test: User \"$test_user5\" should no longer exist." );
    ok( $user->delete( $test_user6 ),
        "Connection Test: User \"$test_user6\" deleted successfully." );
    ok( ! $user->exists( $test_user6 ),
        "Connection Test: User \"$test_user6\" should no longer exist." );
    ok( $user->delete( $test_user7 ),
        "Connection Test: User \"$test_user7\" deleted successfully." );
    ok( ! $user->exists( $test_user7 ),
        "Connection Test: User \"$test_user7\" should no longer exist." );
}
#}}}

1;
