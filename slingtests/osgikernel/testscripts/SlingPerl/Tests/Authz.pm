#!/usr/bin/perl

package Tests::Authz;

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Authz;
use Sling::Content;
use Sling::Group;
use Sling::User;
use Test::More;
#}}}

#{{{sub run_regression_test

=pod

=head2 run_regression_test

Run regression tests for the authz object.

=cut

sub run_regression_test {
    my ( $authn, $verbose, $log ) = @_;
    # test user name:
    my $test_user1 = "authz_test_user_1_$$";
    my $test_user2 = "authz_test_user_2_$$";
    my $test_user3 = "authz_test_user_3_$$";
    my $test_user4 = "authz_test_user_4_$$";
    # test user pass:
    my $test_pass = "pass";
    # test properties:
    my @test_properties;
    # test group name:
    my $test_group1 = "g-authz_test_group_1_$$";
    my $test_group2 = "g-authz_test_group_2_$$";
    # test content:
    my $test_content = "authz_test_content_$$";
    # Sling user object:
    my $authz = new Sling::Authz( $authn, $verbose, $log );
    my $content = new Sling::Content( $authn, $verbose, $log );
    my $group = new Sling::Group( $authn, $verbose, $log );
    my $user = new Sling::User( $authn, $verbose, $log );

    my $super_user = $$authn->{ 'Username' };
    my $super_pass = $$authn->{ 'Password' };

    # Create objects:
    ok( defined $authz,
        "Authz Test: Sling Authz Object successfully created." );
    ok( defined $content,
        "Authz Test: Sling Content Object successfully created." );
    ok( defined $group,
        "Authz Test: Sling Group Object successfully created." );
    ok( defined $user,
        "Authz Test: Sling User Object successfully created." );

    # create users:
    ok( $user->add( $test_user1, $test_pass, \@test_properties ),
        "Authz Test: User \"$test_user1\" added successfully." );
    ok( $user->exists( $test_user1 ),
        "Authz Test: User \"$test_user1\" exists." );
    ok( $user->add( $test_user2, $test_pass, \@test_properties ),
        "Authz Test: User \"$test_user2\" added successfully." );
    ok( $user->exists( $test_user2 ),
        "Authz Test: User \"$test_user2\" exists." );
    ok( $user->add( $test_user3, $test_pass, \@test_properties ),
        "Authz Test: User \"$test_user3\" added successfully." );
    ok( $user->exists( $test_user3 ),
        "Authz Test: User \"$test_user3\" exists." );
    ok( $user->add( $test_user4, $test_pass, \@test_properties ),
        "Authz Test: User \"$test_user4\" added successfully." );
    ok( $user->exists( $test_user4 ),
        "Authz Test: User \"$test_user4\" exists." );

    # create groups:
    ok( $group->add( $test_group1, \@test_properties ),
        "Authz Test: Group \"$test_group1\" added successfully." );
    ok( $group->exists( $test_group1 ),
        "Authz Test: Group \"$test_group1\" exists." );
    ok( $group->add( $test_group2, \@test_properties ),
        "Authz Test: Group \"$test_group2\" added successfully." );
    ok( $group->exists( $test_group2 ),
        "Authz Test: Group \"$test_group2\" exists." );

    # Add members to groups:
    ok( $group->member_add( $test_group1, $test_user3 ),
        "Authz Test: Member \"$test_user3\" added to \"$test_group1\"." );
    ok( $group->member_exists( $test_group1, $test_user3 ),
        "Authz Test: Member \"$test_user3\" exists in \"$test_group1\"." );
    ok( $group->member_add( $test_group2, $test_user4 ),
        "Authz Test: Member \"$test_user4\" added to \"$test_group2\"." );
    ok( $group->member_exists( $test_group2, $test_user4 ),
        "Authz Test: Member \"$test_user4\" exists in \"$test_group2\"." );

    # Add content:
    ok( $content->add( $test_content, \@test_properties ),
        "Authz Test: Content \"$test_content\" added successfully." );
    ok( $content->exists( $test_content ),
        "Authz Test: Content \"$test_content\" exists." );

    # Add ACLs - allow read and write:
    my @grant_privileges = ( "read", "write" );
    my @deny_privileges = ();
    ok( $authz->modify_privileges( $test_content, $test_user1, \@grant_privileges, \@deny_privileges ),
        "Authz Test: ACL applied successfully for user: \"$test_user1\"." );
    ok( $authz->modify_privileges( $test_content, $test_group1, \@grant_privileges, \@deny_privileges ),
        "Authz Test: ACL applied successfully for group: \"$test_group1\"." );

    # Add ACLs - read only:
    @grant_privileges = ( "read" );
    @deny_privileges = ( "write" );
    ok( $authz->modify_privileges( $test_content, $test_user2, \@grant_privileges, \@deny_privileges ),
        "Authz Test: ACL applied successfully for user: \"$test_user2\"." );
    ok( $authz->modify_privileges( $test_content, $test_group2, \@grant_privileges, \@deny_privileges ),
        "Authz Test: ACL applied successfully for group: \"$test_group2\"." );
    
    # Check user 1 can write:
    ok( $$authn->switch_user( $test_user1, $test_pass ),
        "Authz Test: Successfully switched to user: \"$test_user1\" with basic auth" );
    @test_properties = ( "edit=$test_user1" );
    ok( $content->add( $test_content, \@test_properties ),
        "Authz Test: Content \"$test_content\" updated successfully by user: \"$test_user1\"." );

    # Check user 2 can't write:
    ok( $$authn->switch_user( $test_user2, $test_pass ),
        "Authz Test: Successfully switched to user: \"$test_user2\" with basic auth" );
    @test_properties = ( "edit=$test_user2" );
    ok( ! $content->add( $test_content, \@test_properties ),
        "Authz Test: Content \"$test_content\" could not be updated by user: \"$test_user2\"." );

    # Check user 3 can write:
    ok( $$authn->switch_user( $test_user3, $test_pass ),
        "Authz Test: Successfully switched to user: \"$test_user3\" with basic auth" );
    @test_properties = ( "edit=$test_user3" );
    ok( $content->add( $test_content, \@test_properties ),
        "Authz Test: Content \"$test_content\" updated successfully by user: \"$test_user3\"." );

    # Check user 4 can't write:
    ok( $$authn->switch_user( $test_user4, $test_pass ),
        "Authz Test: Successfully switched to user: \"$test_user4\" with basic auth" );
    @test_properties = ( "edit=$test_user4" );
    ok( ! $content->add( $test_content, \@test_properties ),
        "Authz Test: Content \"$test_content\" could not be updated by user: \"$test_user4\"." );

    ok( $$authn->switch_user( $super_user, $super_pass ),
        "Authz Test: Successfully switched to user: \"$super_user\" with basic auth" );

    # Delete content
    ok( $content->delete( $test_content ),
        "Authz Test: Content \"$test_content\" deleted successfully." );
    ok( ! $content->exists( $test_content ),
        "Authz Test: Content \"$test_content\" should no longer exist." );

    # Remove members from groups:
    ok( $group->member_delete( $test_group1, $test_user3 ),
        "Authz Test: Member \"$test_user3\" deleted from \"$test_group1\"." );
    ok( ! $group->member_exists( $test_group1, $test_user3 ),
        "Authz Test: Member \"$test_user3\" no longer exists in \"$test_group1\"." );
    ok( $group->member_delete( $test_group2, $test_user4 ),
        "Authz Test: Member \"$test_user4\" deleted from \"$test_group2\"." );
    ok( ! $group->member_exists( $test_group2, $test_user4 ),
        "Authz Test: Member \"$test_user4\" no longer exists in \"$test_group2\"." );

    # Cleanup Groups:
    ok( $group->delete( $test_group1 ),
        "Authz Test: Group \"$test_group1\" deleted successfully." );
    ok( ! $group->exists( $test_group1 ),
        "Authz Test: Group \"$test_group1\" should no longer exist." );
    ok( $group->delete( $test_group2 ),
        "Authz Test: Group \"$test_group2\" deleted successfully." );
    ok( ! $group->exists( $test_group2 ),
        "Authz Test: Group \"$test_group2\" should no longer exist." );

    # Delete users:
    ok( $user->delete( $test_user1 ),
        "Authz Test: User \"$test_user1\" deleted successfully." );
    ok( ! $user->exists( $test_user1 ),
        "Authz Test: User \"$test_user1\" should no longer exist." );
    ok( $user->delete( $test_user2 ),
        "Authz Test: User \"$test_user2\" deleted successfully." );
    ok( ! $user->exists( $test_user2 ),
        "Authz Test: User \"$test_user2\" should no longer exist." );
}
#}}}

1;
