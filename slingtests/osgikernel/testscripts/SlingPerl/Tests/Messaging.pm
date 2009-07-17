#!/usr/bin/perl

package Tests::Messaging;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Messaging;
use Sling::User;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the messaging object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test users:
    my $test_user1 = "messaging_test_user_1_$$";
    my $test_user2 = "messaging_test_user_2_$$";
    # test user pass:
    my $test_pass = "pass";
    # test properties:
    my @test_properties;
    # types:
    my @types;
    # Sling messaging object:
    my $messaging = new Sling::Messaging( $authn, $verbose, $log );
    # Sling user object:
    my $user = new Sling::User( $authn, $verbose, $log );

    my $super_user = $$authn->{ 'Username' };
    my $super_pass = $$authn->{ 'Password' };

    # Run tests:
    ok( defined $messaging,
        "Messaging Test: Sling Messaging Object successfully created." );
    ok( defined $user,
        "Messaging Test: Sling User Object successfully created." );

    ok( $user->add( $test_user1, $test_pass, \@test_properties ),
        "Messaging Test: User \"$test_user1\" added successfully." );
    ok( $user->exists( $test_user1 ),
        "Messaging Test: User \"$test_user1\" exists." );
    ok( $user->add( $test_user2, $test_pass, \@test_properties ),
        "Messaging Test: User \"$test_user2\" added successfully." );
    ok( $user->exists( $test_user2 ),
        "Messaging Test: User \"$test_user2\" exists." );

    # Create and send message:
    ok( $$authn->switch_user( $test_user1, $test_pass ),
        "Messaging Test: Successfully switched to user: \"" . $test_user1 . "\" with basic auth" );
    my $messageId;
    ok( $messageId = $messaging->create( $test_user2, "internal" ),
        "Messaging Test: Successfully created internal message for user: \"" . $test_user2 . "\"" );
    ok( $messaging->send( $messageId ),
        "Messaging Test: Successfully sent message with id: \"" . $messageId . "\"" );
    my $sort_by;
    my $order;
    ok( $messaging->list( "outbox", $sort_by, $order ) == 1,
        "Messaging Test: Outbox for user \"$test_user1\" now contains 1 message" );

    # Check wessage was received:
    ok( $$authn->switch_user( $test_user2, $test_pass ),
        "Messaging Test: Successfully switched to user: \"" . $test_user2 . "\" with basic auth" );
    ok( $messaging->list( "inbox", $sort_by, $order ) == 1,
        "Messaging Test: Inbox for user \"$test_user2\" now contains 1 message" );

    ok( $$authn->switch_user( $super_user, $super_pass ),
        "Messaging Test: Successfully switched to user: \"$super_user\" with basic auth" );
    ok( $user->delete( $test_user1 ),
        "Messaging Test: User \"$test_user1\" deleted successfully." );
    ok( ! $user->exists( $test_user1 ),
        "Messaging Test: User \"$test_user1\" should no longer exist." );
    ok( $user->delete( $test_user2 ),
        "Messaging Test: User \"$test_user2\" deleted successfully." );
    ok( ! $user->exists( $test_user2 ),
        "Messaging Test: User \"$test_user2\" should no longer exist." );
}
#}}}

1;
