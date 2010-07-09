#!/usr/bin/perl

package Sling::MessagingUtil;

=head1 NAME

MessagingUtil - Utility library returning strings representing Rest queries that
perform messaging operations in the system.

=head1 ABSTRACT

MessagingUtil perl library essentially provides the request strings needed to
interact with messaging functionality exposed over the system rest interfaces.

Each interaction has a setup and eval method. setup provides the request,
whilst eval interprets the response to give further information about the
result of performing the request.

=cut

#{{{imports
use strict;
use lib qw ( .. );
#}}}

#{{{sub create_setup

=pod

=head2 create_setup

Returns a textual representation of the request needed to create a message of a
specified type for a specified user in the system.

=cut

sub create_setup {
    my ( $baseURL, $name, $type ) = @_;
    die "No base URL provided to create message at!" unless defined $baseURL;
    die "No name provided to send message to!" unless defined $name;
    die "No message type specified!" unless defined $type;
    my $postVariables = "\$postVariables = ['sakai:type','$type','sakai:to','$name','sakai:sendstate','pending','sakai:messagebox','drafts']";
    return "post $baseURL/~$user/message.create.html $postVariables";
}
#}}}

#{{{sub create_eval

=pod

=head2 create_eval

Check result of attempting to create message

=cut

sub create_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub list_setup

=pod

=head2 list_setup

Returns a textual representation of the request needed to list messages for the
logged in user, with the ability to specify a particular message box and order
the messages by a particular property in a particular direction.

=cut

sub list_setup {
    my ( $baseURL, $box, $sort_by, $order ) = @_;
    die "No base URL provided to obtain message list for!" unless defined $baseURL;
    die "No box specified to retrieve message list for!" unless defined $baseURL;
    my $sort_string;
    if ( defined $sort_by ) {
        $sort_string = "sortOn=$sort_by";
	if ( defined $order ) {
	    $sort_string .= "&sortOrder=$order";
	}
    }
    my $request;
    if ( $box =~ /^all$/ ) {
        $request = "get $baseURL/~$user/message/all.json";
	$request .= ( defined $sort_string ? "?$sort_string" : "" );
    }
    else {
        $request = "get $baseURL/~$user/message/box.json?box=$box";
	$request .= ( defined $sort_string ? "&$sort_string" : "" );
    }
    return $request;
}
#}}}

#{{{sub list_eval

=pod

=head2 list_eval

Check result of obtaining message list.

=cut

sub list_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

#{{{sub send_setup

=pod

=head2 send_setup

Returns a textual representation of the request needed to send the message with
the specified message Id.

=cut

sub send_setup {
    my ( $baseURL, $messageId ) = @_;
    die "No base URL provided to send message at!" unless defined $baseURL;
    die "No message Id specified!" unless defined $messageId;
    my $postVariables = "\$postVariables = ['sakai:messagebox','outbox']";
    return "post $baseURL/~$user/message/$messageId.html $postVariables";
}
#}}}

#{{{sub send_eval

=pod

=head2 send_eval

Check result of performing message send.

=cut

sub send_eval {
    my ( $res ) = @_;
    return ( $$res->code =~ /^200$/ );
}
#}}}

1;
