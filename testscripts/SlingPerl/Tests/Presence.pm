#!/usr/bin/perl

package Tests::Presence;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Connection;
use Sling::Presence;
use Sling::User;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the presence object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test users:
    my $test_user1 = "presence_test_user_1_$$";
    my $test_user2 = "presence_test_user_2_$$";
    my $test_user3 = "presence_test_user_3_$$";
    my $test_user4 = "presence_test_user_4_$$";
    # test user pass:
    my $test_pass = "pass";
    # test properties:
    my @test_properties;
    # types:
    my @types;
    # Sling presence object:
    my $presence = new Sling::Presence( $authn, $verbose, $log );
    # Sling connection object:
    my $connection = new Sling::Connection( $authn, $verbose, $log );
    # Sling user object:
    my $user = new Sling::User( $authn, $verbose, $log );

    my $super_user = $$authn->{ 'Username' };
    my $super_pass = $$authn->{ 'Password' };

    # Run tests:
    ok( defined $user,
        "Presence Test: Sling User Object successfully created." );
    ok( defined $connection,
        "Presence Test: Sling Connection Object successfully created." );
    ok( defined $presence,
        "Presence Test: Sling Presence Object successfully created." );

    ok( $user->add( $test_user1, $test_pass, \@test_properties ),
        "Presence Test: User \"$test_user1\" added successfully." );
    ok( $user->exists( $test_user1 ),
        "Presence Test: User \"$test_user1\" exists." );
    ok( $user->add( $test_user2, $test_pass, \@test_properties ),
        "Presence Test: User \"$test_user2\" added successfully." );
    ok( $user->exists( $test_user2 ),
        "Presence Test: User \"$test_user2\" exists." );
    ok( $user->add( $test_user3, $test_pass, \@test_properties ),
        "Presence Test: User \"$test_user3\" added successfully." );
    ok( $user->exists( $test_user3 ),
        "Presence Test: User \"$test_user3\" exists." );
    ok( $user->add( $test_user4, $test_pass, \@test_properties ),
        "Presence Test: User \"$test_user4\" added successfully." );
    ok( $user->exists( $test_user4 ),
        "Presence Test: User \"$test_user4\" exists." );

    # Issue invitations:
    ok( $$authn->switch_user( $test_user1, $test_pass ),
        "Presence Test: Successfully switched to user: \"" . $test_user1 . "\" with basic auth" );
    ok( $connection->invite( $test_user2, \@types ),
        "Presence Test: User \"$test_user1\" successfully invited: \"$test_user2\"" );
    ok( $connection->invite( $test_user3, \@types ),
        "Presence Test: User \"$test_user1\" successfully invited: \"$test_user3\"" );
    ok( $connection->invite( $test_user4, \@types ),
        "Presence Test: User \"$test_user1\" successfully invited: \"$test_user4\"" );

    # Perform presence tests:
    ok( $$authn->switch_user( $test_user1, $test_pass ),
        "Presence Test: Successfully switched to user: \"$test_user1\" with basic auth" );
    ok( $presence->status(),
        "Presence Test: Successfully fetched presence status for: \"$test_user1\"" );
    ok( $presence->update( "office", "online" ),
        "Presence Test: Successfully updated presence status of: \"$test_user1\"" );
    ok( $presence->delete(),
        "Presence Test: Successfully cleared presence status of: \"$test_user1\"" );
    ok( $presence->contacts() == 0,
        "Presence Test: Successfully fetched presence status of contacts for: \"$test_user1\", though no contacts exist yet" );

    # Respond to invitations:
    ok( $$authn->switch_user( $test_user2, $test_pass ),
        "Presence Test: Successfully switched to user: \"$test_user2\" with basic auth" );
    ok( $connection->accept( $test_user1 ),
        "Presence Test: User \"$test_user2\" successfully accepted invitation from: \"$test_user1\"" );
    ok( $$authn->switch_user( $test_user1, $test_pass ),
        "Presence Test: Successfully switched to user: \"$test_user1\" with basic auth" );
    ok( $presence->contacts() == 1,
        "Presence Test: Successfully fetched presence status of contacts for: \"$test_user1\", should be one contact" );

    ok( $$authn->switch_user( $test_user3, $test_pass ),
        "Presence Test: Successfully switched to user: \"$test_user3\" with basic auth" );
    ok( $connection->accept( $test_user1 ),
        "Presence Test: User \"$test_user3\" successfully accepted invitation from: \"$test_user1\"" );
    ok( $$authn->switch_user( $test_user1, $test_pass ),
        "Presence Test: Successfully switched to user: \"$test_user1\" with basic auth" );
    ok( $presence->contacts() == 2,
        "Presence Test: Successfully fetched presence status of contacts for: \"$test_user1\", should be two contacts" );

    ok( $$authn->switch_user( $test_user4, $test_pass ),
        "Presence Test: Successfully switched to user: \"$test_user4\" with basic auth" );
    ok( $connection->accept( $test_user1 ),
        "Presence Test: User \"$test_user4\" successfully accepted invitation from: \"$test_user1\"" );
    ok( $$authn->switch_user( $test_user1, $test_pass ),
        "Presence Test: Successfully switched to user: \"$test_user1\" with basic auth" );
    ok( $presence->contacts() == 3,
        "Presence Test: Successfully fetched presence status of contacts for: \"$test_user1\", should be three contacts" );

    ok( $connection->remove( $test_user2 ),
        "Presence Test: User \"$test_user1\" successfully removed connection to user \"$test_user2\"" );
    ok( $presence->contacts() == 2,
        "Presence Test: Successfully fetched presence status of contacts for: \"$test_user1\", should be two contacts" );
    ok( $connection->remove( $test_user3 ),
        "Presence Test: User \"$test_user1\" successfully removed connection to user \"$test_user3\"" );
    ok( $presence->contacts() == 1,
        "Presence Test: Successfully fetched presence status of contacts for: \"$test_user1\", should be one contact" );
    ok( $connection->remove( $test_user4 ),
        "Presence Test: User \"$test_user1\" successfully removed connection to user \"$test_user4\"" );
    ok( $presence->contacts() == 0,
        "Presence Test: Successfully fetched presence status of contacts for: \"$test_user1\", though no contacts should exist" );

    ok( $$authn->switch_user( $super_user, $super_pass ),
        "Presence Test: Successfully switched to user: \"$super_user\" with basic auth" );
    ok( $user->delete( $test_user1 ),
        "Presence Test: User \"$test_user1\" deleted successfully." );
    ok( ! $user->exists( $test_user1 ),
        "Presence Test: User \"$test_user1\" should no longer exist." );
    ok( $user->delete( $test_user2 ),
        "Presence Test: User \"$test_user2\" deleted successfully." );
    ok( ! $user->exists( $test_user2 ),
        "Presence Test: User \"$test_user2\" should no longer exist." );
    ok( $user->delete( $test_user3 ),
        "Presence Test: User \"$test_user3\" deleted successfully." );
    ok( ! $user->exists( $test_user3 ),
        "Presence Test: User \"$test_user3\" should no longer exist." );
    ok( $user->delete( $test_user4 ),
        "Presence Test: User \"$test_user4\" deleted successfully." );
    ok( ! $user->exists( $test_user4 ),
        "Presence Test: User \"$test_user4\" should no longer exist." );
}
#}}}

1;
