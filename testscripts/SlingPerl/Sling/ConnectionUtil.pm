#!/usr/bin/perl

package Sling::ConnectionUtil;

=head1 NAME

ConnectionUtil - Utility library returning strings representing Rest queries that
perform connection operations in the system.

=head1 ABSTRACT

ConnectionUtil perl library essentially provides the request strings needed to
interact with connection functionality exposed over the system rest interfaces.

Each interaction has a setup and eval method. setup provides the request,
whilst eval interprets the response to give further information about the
result of performing the request.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Sling::URL;
#}}}

#{{{sub accept_setup

=pod

=head2 accept_setup

Returns a textual representation of the request needed to accept an invitation
to connect.

=cut

sub accept_setup {
    my ( $baseURL, $accept ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    die "No connection to accept given!" unless defined $accept;
    my $postVariables = "\$postVariables = []";
    return "post $baseURL/_user/contacts/$accept.accept.html $postVariables";
}
#}}}

#{{{sub accept_eval

=pod

=head2 accept_eval

Returns true if the result returned from issuing the request generated in
accept_setup indicates the invitation was accepted successfully, else false.

=cut

sub accept_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub block_setup

=pod

=head2 block_setup

Returns a textual representation of the request needed to block an invitation
to connect.

=cut

sub block_setup {
    my ( $baseURL, $block ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    die "No connection to block given!" unless defined $block;
    my $postVariables = "\$postVariables = []";
    return "post $baseURL/_user/contacts/$block.block.html $postVariables";
}
#}}}

#{{{sub block_eval

=pod

=head2 block_eval

Returns true if the result returned from issuing the request generated in
block_setup indicates blocking the invitation was successful, else false.

=cut

sub block_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub cancel_setup

=pod

=head2 cancel_setup

Returns a textual representation of the request needed to cancel an invitation
to connect.

=cut

sub cancel_setup {
    my ( $baseURL, $cancel ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    die "No connection to cancel given!" unless defined $cancel;
    my $postVariables = "\$postVariables = []";
    return "post $baseURL/_user/contacts/$cancel.cancel.html $postVariables";
}
#}}}

#{{{sub cancel_eval

=pod

=head2 cancel_eval

Returns true if the result returned from issuing the request generated in
cancel_setup indicates cancelling the invitation was successful, else false.

=cut

sub cancel_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub ignore_setup

=pod

=head2 ignore_setup

Returns a textual representation of the request needed to ignore an invitation
to connect.

=cut

sub ignore_setup {
    my ( $baseURL, $ignore ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    die "No connection to ignore given!" unless defined $ignore;
    my $postVariables = "\$postVariables = []";
    return "post $baseURL/_user/contacts/$ignore.ignore.html $postVariables";
}
#}}}

#{{{sub ignore_eval

=pod

=head2 ignore_eval

Returns true if the result returned from issuing the request generated in
ignore_setup indicates ignoring the invitation was successful, else false.

=cut

sub ignore_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub invite_setup

=pod

=head2 invite_setup

Returns a textual representation of the request needed to invite a user to
connect with a particular type or set of types defined for the connection.

=cut

sub invite_setup {
    my ( $baseURL, $invite, $types ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    die "No connection to invite given!" unless defined $invite;
    my $postVariables = "\$postVariables = [";
    foreach my $type ( @{ $types } ) {
        $postVariables .= "'types','$type',";
    }
    $postVariables =~ s/,$//;
    $postVariables .= "]";
    return "post $baseURL/_user/contacts/$invite.invite.html $postVariables";
}
#}}}

#{{{sub invite_eval

=pod

=head2 invite_eval

Returns true if the result returned from issuing the request generated in
invite_setup indicates the invitation was successful, else false.

=cut

sub invite_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^201$/ );
}
#}}}

#{{{sub list_accepted_setup

=pod

=head2 list_accepted_setup

Returns a textual representation of the request needed to list connections for
the current user that are in an accepted state.

=cut

sub list_accepted_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    return "get $baseURL/_user/contacts/accepted.json";
}
#}}}

#{{{sub list_accepted_eval

=pod

=head2 list_accepted_eval

Returns true if the result returned from issuing the request generated in
list_accepted_setup indicates the list was returned successfully, else false.

=cut

sub list_accepted_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub list_all_setup

=pod

=head2 list_all_setup

Returns a textual representation of the request needed to list all connections
for the current user in any state.

=cut

sub list_all_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    return "get $baseURL/_user/contacts/all.json";
}
#}}}

#{{{sub list_all_eval

=pod

=head2 list_all_eval

Returns true if the result returned from issuing the request generated in
list_all_setup indicates the list was returned successfully, else false.

=cut

sub list_all_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub list_blocked_setup

=pod

=head2 list_blocked_setup

Returns a textual representation of the request needed to list connections for
the current user that are in a blocked state.

=cut

sub list_blocked_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    return "get $baseURL/_user/contacts/blocked.json";
}
#}}}

#{{{sub list_blocked_eval

=pod

=head2 list_blocked_eval

Returns true if the result returned from issuing the request generated in
list_blocked_setup indicates the list was returned successfully, else false.

=cut

sub list_blocked_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub list_ignored_setup

=pod

=head2 list_ignored_setup

Returns a textual representation of the request needed to list connections for
the current user that are in an ignored state.

=cut

sub list_ignored_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    return "get $baseURL/_user/contacts/ignored.json";
}
#}}}

#{{{sub list_ignored_eval

=pod

=head2 list_ignored_eval

Returns true if the result returned from issuing the request generated in
list_ignored_setup indicates the list was returned successfully, else false.

=cut

sub list_ignored_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub list_invited_setup

=pod

=head2 list_invited_setup

Returns a textual representation of the request needed to list connections for
the current user that are in an invited state.

=cut

sub list_invited_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    return "get $baseURL/_user/contacts/invited.json";
}
#}}}

#{{{sub list_invited_eval

=pod

=head2 list_invited_eval

Returns true if the result returned from issuing the request generated in
list_invited_setup indicates the list was returned successfully, else false.

=cut

sub list_invited_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub list_pending_setup

=pod

=head2 list_pending_setup

Returns a textual representation of the request needed to list connections for
the current user that are in a pending state.

=cut

sub list_pending_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    return "get $baseURL/_user/contacts/pending.json";
}
#}}}

#{{{sub list_pending_eval

=pod

=head2 list_pending_eval

Returns true if the result returned from issuing the request generated in
list_pending_setup indicates the list was returned successfully, else false.

=cut

sub list_pending_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub list_rejected_setup

=pod

=head2 list_rejected_setup

Returns a textual representation of the request needed to list connections for
the current user that are in a rejected state.

=cut

sub list_rejected_setup {
    my ( $baseURL ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    return "get $baseURL/_user/contacts/rejected.json";
}
#}}}

#{{{sub list_rejected_eval

=pod

=head2 list_rejected_eval

Returns true if the result returned from issuing the request generated in
list_rejected_setup indicates the list was returned successfully, else false.

=cut

sub list_rejected_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub reject_setup

=pod

=head2 reject_setup

Returns a textual representation of the request needed to reject an invitation
to connect.

=cut

sub reject_setup {
    my ( $baseURL, $reject ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    die "No connection to reject given!" unless defined $reject;
    my $postVariables = "\$postVariables = []";
    return "post $baseURL/_user/contacts/$reject.reject.html $postVariables";
}
#}}}

#{{{sub reject_eval

=pod

=head2 reject_eval

Returns true if the result returned from issuing the request generated in
reject_setup indicates rejecting the invitation was successful, else false.

=cut

sub reject_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub remove_setup

=pod

=head2 remove_setup

Returns a textual representation of the request needed to remove an invitation
to connect.

=cut

sub remove_setup {
    my ( $baseURL, $remove ) = @_;
    die "No base URL provided to connect at!" unless defined $baseURL;
    die "No connection to remove given!" unless defined $remove;
    my $postVariables = "\$postVariables = []";
    return "post $baseURL/_user/contacts/$remove.remove.html $postVariables";
}
#}}}

#{{{sub remove_eval

=pod

=head2 remove_eval

Returns true if the result returned from issuing the request generated in
remove_setup indicates removing the invitation was successful, else false.

=cut

sub remove_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

1;
