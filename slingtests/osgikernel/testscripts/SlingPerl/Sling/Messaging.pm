#!/usr/bin/perl

package Sling::Messaging;

=head1 NAME

Messaging - messaging related functionality for Sakai implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST messaging methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use JSON;
use Text::CSV;
use Sling::Print;
use Sling::Request;
use Sling::MessagingUtil;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Messaging object.

=cut

sub new {
    my ( $class, $authn, $verbose, $log ) = @_;
    die "no authn provided!" unless defined $authn;
    my $response;
    my $messaging = { BaseURL => $$authn->{ 'BaseURL' },
                      Authn => $authn,
		      Message => "",
		      Response => \$response,
		      Verbose => $verbose,
		      Log => $log };
    bless( $messaging, $class );
    return $messaging;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $search, $message, $response ) = @_;
    $search->{ 'Message' } = $message;
    $search->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub create
sub create {
    my ( $messaging, $name, $type ) = @_;
    my $res = Sling::Request::request( \$messaging,
        Sling::MessagingUtil::create_setup( $messaging->{ 'BaseURL' }, $name, $type ) );
    my $success = Sling::MessagingUtil::create_eval( $res );
    my $message = "Message of type \"$type\" for \"$name\" was ";
    if ( $success ) {
        my $messageId = from_json( $$res->content )->{ 'id' };
        $message .= "successfully created with id: \"$messageId\"!";
        $success = $messageId;
    }
    else {
        $message .= "not successfully created!";
    }
    $messaging->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub list
sub list {
    my ( $messaging, $box, $sort_by, $order ) = @_;
    my $res = Sling::Request::request( \$messaging,
        Sling::MessagingUtil::list_setup( $messaging->{ 'BaseURL' }, $box, $sort_by, $order ) );
    my $success = Sling::MessagingUtil::list_eval( $res );
    my $message;
    if ( $success ) {
        my $messages = from_json( $$res->content );
	my $message_count = $messages->{ 'total' };
	$message = "Found \"$message_count\" message(s):";
	foreach my $message_details ( @{ $messages->{ 'results' } } ) {
            $message .= "\n* " . $message_details->{ 'id' };
	}
	$success = $message_count;
    }
    else {
        $message = "Problem fetching message list for box \"$box\"!";
    }
    $messaging->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub send
sub send {
    my ( $messaging, $messageId ) = @_;
    my $res = Sling::Request::request( \$messaging,
        Sling::MessagingUtil::send_setup( $messaging->{ 'BaseURL' }, $messageId ) );
    my $success = Sling::MessagingUtil::send_eval( $res );
    my $message = "Message (id: \"$messageId\") was ";
    $message .= ( $success ? "" : "not " ) . "successfully sent!";
    $messaging->set_results( "$message", $res );
    return $success;
}
#}}}

1;
