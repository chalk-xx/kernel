#!/usr/bin/perl

package Tests::User;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Group;
use Sling::User;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the user object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test user name:
    my $test_user = "user_test_user_$$";
    # test user pass:
    my $test_pass = "pass";
    # test properties:
    my @test_properties;
    # Sling user object:
    my $user = new Sling::User( $authn, $verbose, $log );

    # test group name:
    my $test_group = "g-user_test_group_$$";
    # Sling group object:
    my $group = new Sling::Group( $authn, $verbose, $log );

    my $super_user = $$authn->{ 'Username' };
    my $super_pass = $$authn->{ 'Password' };

    # Run tests:
    ok( defined $user,
        "User Test: Sling User Object successfully created." );
    ok( defined $group,
        "User Test: Sling Group Object successfully created." );

    # admin user should be able to fetch me feed:
    ok( $user->me(),
        "User Test: user: \"$super_user\" successfully fetched me feed." );

    # add user:
    ok( $user->add( $test_user, $test_pass, \@test_properties ),
        "User Test: User \"$test_user\" added successfully." );
    ok( $user->exists( $test_user ),
        "User Test: User \"$test_user\" exists." );

    # Check users can not start with "g-":
    ok( ! $user->add( "g-$test_user", $test_pass, \@test_properties ),
        "User Test: User \"g-$test_user\" creation denied." );
    ok( ! $user->exists( "g-$test_user" ),
        "User Test: User \"g-$test_user\" should not exist." );

    # Check can update properties:
    @test_properties = ( "user_test_editor=$super_user" );
    ok( $user->update( $test_user, \@test_properties ),
        "User Test: User \"$test_user\" updated successfully." );

    # Check can update properties after addition pf user to group:
    # http://jira.sakaiproject.org/browse/KERN-270
    # create group:
    ok( $group->add( $test_group, \@test_properties ),
        "User Test: Group \"$test_group\" added successfully." );
    ok( $group->exists( $test_group ),
        "User Test: Group \"$test_group\" exists." );
    # Add member to group:
    ok( $group->member_add( $test_group, $test_user ),
        "User Test: Member \"$test_user\" added to \"$test_group\"." );
    ok( $group->member_exists( $test_group, $test_user ),
        "User Test: Member \"$test_user\" exists in \"$test_group\"." );
    # Check can still update properties:
    @test_properties = ( "user_test_edit_after_group_join=true" );
    ok( $user->update( $test_user, \@test_properties ),
        "User Test: User \"$test_user\" updated successfully." );
    # Delete test user from group:
    ok( $group->member_delete( $test_group, $test_user ),
        "User Test: Member \"$test_user\" deleted from \"$test_group\"." );
    ok( ! $group->member_exists( $test_group, $test_user ),
        "User Test: Member \"$test_user\" should no longer exist in \"$test_group\"." );
    # Cleanup Group:
    ok( $group->delete( $test_group ),
        "User Test: Group \"$test_group\" deleted successfully." );
    ok( ! $group->exists( $test_group ),
        "User Test: Group \"$test_group\" should no longer exist." );

    # Switch to test_user
    ok( $$authn->switch_user( $test_user, $test_pass ),
        "User Test: Successfully switched to user: \"$test_user\" with basic auth" );

    # ordinary user should be able to fetch me feed:
    ok( $user->me(),
        "User Test: user: \"$test_user\" successfully fetched me feed." );

    # Check can update properties:
    @test_properties = ( "user_test_editor=$test_user" );
    ok( $user->update( $test_user, \@test_properties ),
        "User Test: User \"$test_user\" updated successfully." );

    # switch back to admin user:
    ok( $$authn->switch_user( $super_user, $super_pass ),
        "User Test: Successfully switched to user: \"$super_user\" with basic auth" );

    # Check user deletion:
    ok( $user->delete( $test_user ),
        "User Test: User \"$test_user\" deleted successfully." );
    ok( ! $user->exists( $test_user ),
        "User Test: User \"$test_user\" should no longer exist." );
}
#}}}

1;
