#!/usr/bin/perl

package Sling::UserUtil;

=head1 NAME

UserUtil - Utility library returning strings representing Rest queries that
perform user related actions in the system.

=head1 ABSTRACT

UserUtil perl library essentially provides the request strings needed to
interact with user functionality exposed over the system rest interfaces.

Each interaction has a setup and eval method. setup provides the request,
whilst eval interprets the response to give further information about the
result of performing the request.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::Util;
#}}}

#{{{sub add_setup

=pod

=head2 add_setup

Returns a textual representation of the request needed to add the user to the
system.

=cut

sub add_setup {
    my ( $baseURL, $actOnUser, $actOnPass, $actOnEmail, $actOnFirst, $actOnLast ) = @_;
    die "No base url defined to add against!" unless defined $baseURL;
    die "No user name defined to add!" unless defined $actOnUser;
    die "No user password defined to add for user $actOnUser!" unless defined $actOnPass;
    die "No user email defined to add for user $actOnUser!" unless defined $actOnEmail;
    $actOnFirst = $actOnUser unless defined $actOnFirst;
    $actOnLast = $actOnUser unless defined $actOnLast;
    $actOnUser = Sling::Util::urlencode( $actOnUser );
    $actOnPass = Sling::Util::urlencode( $actOnPass );
    $actOnFirst = Sling::Util::urlencode( $actOnFirst );
    $actOnLast = Sling::Util::urlencode( $actOnLast );
    my $postVariables = "\$postVariables = [':name','$actOnUser','pwd','$actOnPass','pwdConfirm','$actOnPass','firstName','$actOnFirst','lastName','$actOnLast','email','$actOnEmail']";
    return "post $baseURL/system/userManager/user.create.html $postVariables";
}
#}}}

#{{{sub add_eval

=pod

=head2 add_eval

Check result of adding user to the system.

=cut

sub add_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub change_password_setup

=pod

=head2 change_password_setup

Returns a textual representation of the request needed to change the password
of the user in the system.

=cut

sub change_password_setup {
    my ( $baseURL, $actOnUser, $actOnPass, $newPass, $newPassConfirm ) = @_;
    die "No base url defined to add against!" unless defined $baseURL;
    die "No user name defined to change password for!" unless defined $actOnUser;
    die "No current password defined for $actOnUser!" unless defined $actOnPass;
    die "No new password defined for $actOnUser!" unless defined $newPass;
    die "No confirmation of new password defined for $actOnUser!" unless defined $newPassConfirm;
    $actOnUser = Sling::Util::urlencode( $actOnUser );
    $actOnPass = Sling::Util::urlencode( $actOnPass );
    $newPass = Sling::Util::urlencode( $newPass );
    $newPassConfirm = Sling::Util::urlencode( $newPassConfirm );
    my $postVariables = "\$postVariables = ['oldPwd','$actOnPass','newPwd','$newPass','newPwdConfirm','$newPassConfirm']";
    return "post $baseURL/system/userManager/user/$actOnUser.changePassword.html?sling:authRequestLogin=1 $postVariables";
}
#}}}

#{{{sub change_password_eval

=pod

=head2 change_password_eval

Verify whether the change password attempt for the user in the system was successful.

=cut

sub change_password_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub delete_setup

=pod

=head2 delete_setup

Returns a textual representation of the request needed to delete the user from
the system.

=cut

sub delete_setup {
    my ( $baseURL, $actOnUser ) = @_;
    die "No base url defined to delete against!" unless defined $baseURL;
    die "No user name defined to delete!" unless defined $actOnUser;
    $actOnUser = Sling::Util::urlencode( $actOnUser );
    my $postVariables = "\$postVariables = []";
    return "post $baseURL/system/userManager/user/$actOnUser.delete.html?sling:authRequestLogin=1 $postVariables";
}
#}}}

#{{{sub delete_eval

=pod

=head2 delete_eval

Check result of deleting user from the system.

=cut

sub delete_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub exists_setup

=pod

=head2 exists_setup

Returns a textual representation of the request needed to test whether a given
username exists in the system.

=cut

sub exists_setup {
    my ( $baseURL, $actOnUser ) = @_;
    die "No base url to check existence against!" unless defined $baseURL;
    die "No user to check existence of defined!" unless defined $actOnUser;
    $actOnUser = Sling::Util::urlencode( $actOnUser );
    return "get $baseURL/system/userManager/user/$actOnUser.json";
}
#}}}

#{{{sub exists_eval

=pod

=head2 exists_eval

Inspects the result returned from issuing the request generated in exists_setup
returning true if the result indicates the username does exist in the system,
else false.

=cut

sub exists_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

1;
