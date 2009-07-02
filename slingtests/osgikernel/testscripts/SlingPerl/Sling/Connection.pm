#!/usr/bin/perl

package Sling::Connection;

=head1 NAME

Connection - connection related functionality for Sakai implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST connection methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use JSON;
use Sling::Print;
use Sling::Request;
use Sling::ConnectionUtil;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Search object.

=cut

sub new {
    my ( $class, $auth, $verbose, $log ) = @_;
    die "no auth provided!" unless defined $auth;
    my $response;
    my $connection = { BaseURL => $$auth->{ 'BaseURL' },
                       Auth => $auth,
		       Message => "",
		       Response => \$response,
		       Verbose => $verbose,
		       Log => $log };
    bless( $connection, $class );
    return $connection;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $connection, $message, $response ) = @_;
    $connection->{ 'Message' } = $message;
    $connection->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub accept 
sub accept {
    my ( $connection, $accept ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::accept_setup( $connection->{ 'BaseURL' }, $accept ) );
    my $success = Sling::ConnectionUtil::accept_eval( $res );
    my $message = "Invitation: ";
    $message .= ( $success ? "accepted " : "was not accepted " ) . "for \"$accept\"";
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub block 
sub block {
    my ( $connection, $block ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::block_setup( $connection->{ 'BaseURL' }, $block ) );
    my $success = Sling::ConnectionUtil::block_eval( $res );
    my $message = "Invitation: ";
    $message .= ( $success ? "blocked " : "was not blocked " ) . "for \"$block\"";
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub ignore 
sub ignore {
    my ( $connection, $ignore ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::ignore_setup( $connection->{ 'BaseURL' }, $ignore ) );
    my $success = Sling::ConnectionUtil::ignore_eval( $res );
    my $message = "Invitation: ";
    $message .= ( $success ? "ignored " : "was not ignored " ) . "for \"$ignore\"";
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub invite 
sub invite {
    my ( $connection, $invite, $types ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::invite_setup( $connection->{ 'BaseURL' }, $invite, $types ) );
    my $success = Sling::ConnectionUtil::invite_eval( $res );
    my $message = "Invitation: ";
    $message .= ( $success ? "issued " : "was not issued " ) . "to \"$invite\"";
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub list_accepted 
sub list_accepted {
    my ( $connection ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::list_accepted_setup( $connection->{ 'BaseURL' } ) );
    my $success = Sling::ConnectionUtil::list_accepted_eval( $res );
    my $message;
    if ( $success ) {
        my $all = from_json( $$res->content );
	$message = "Found \"" . $all->{ 'total' } . "\" connection(s):";
	foreach my $connection ( @{ $all->{ 'results' } } ) {
            $message .= "\n* " . $connection->{ 'target' } . " " . $connection->{ 'details' }->{ 'sakai:state' };
	}
    }
    else {
        $message = "Problem viewing list!";
    }
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub list_all 
sub list_all {
    my ( $connection ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::list_all_setup( $connection->{ 'BaseURL' } ) );
    my $success = Sling::ConnectionUtil::list_all_eval( $res );
    my $message;
    if ( $success ) {
        my $all = from_json( $$res->content );
	$message = "Found \"" . $all->{ 'total' } . "\" connection(s):";
	foreach my $connection ( @{ $all->{ 'results' } } ) {
            $message .= "\n* " . $connection->{ 'target' } . " " . $connection->{ 'details' }->{ 'sakai:state' };
	}
    }
    else {
        $message = "Problem viewing list!";
    }
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub list_blocked 
sub list_blocked {
    my ( $connection ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::list_blocked_setup( $connection->{ 'BaseURL' } ) );
    my $success = Sling::ConnectionUtil::list_blocked_eval( $res );
    my $message;
    if ( $success ) {
        my $all = from_json( $$res->content );
	$message = "Found \"" . $all->{ 'total' } . "\" connection(s):";
	foreach my $connection ( @{ $all->{ 'results' } } ) {
            $message .= "\n* " . $connection->{ 'target' } . " " . $connection->{ 'details' }->{ 'sakai:state' };
	}
    }
    else {
        $message = "Problem viewing list!";
    }
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub list_ignored 
sub list_ignored {
    my ( $connection ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::list_ignored_setup( $connection->{ 'BaseURL' } ) );
    my $success = Sling::ConnectionUtil::list_ignored_eval( $res );
    my $message;
    if ( $success ) {
        my $all = from_json( $$res->content );
	$message = "Found \"" . $all->{ 'total' } . "\" connection(s):";
	foreach my $connection ( @{ $all->{ 'results' } } ) {
            $message .= "\n* " . $connection->{ 'target' } . " " . $connection->{ 'details' }->{ 'sakai:state' };
	}
    }
    else {
        $message = "Problem viewing list!";
    }
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub list_invited 
sub list_invited {
    my ( $connection ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::list_invited_setup( $connection->{ 'BaseURL' } ) );
    my $success = Sling::ConnectionUtil::list_invited_eval( $res );
    my $message;
    if ( $success ) {
        my $all = from_json( $$res->content );
	$message = "Found \"" . $all->{ 'total' } . "\" connection(s):";
	foreach my $connection ( @{ $all->{ 'results' } } ) {
            $message .= "\n* " . $connection->{ 'target' } . " " . $connection->{ 'details' }->{ 'sakai:state' };
	}
    }
    else {
        $message = "Problem viewing list!";
    }
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub list_pending 
sub list_pending {
    my ( $connection ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::list_pending_setup( $connection->{ 'BaseURL' } ) );
    my $success = Sling::ConnectionUtil::list_pending_eval( $res );
    my $message;
    if ( $success ) {
        my $all = from_json( $$res->content );
	$message = "Found \"" . $all->{ 'total' } . "\" connection(s):";
	foreach my $connection ( @{ $all->{ 'results' } } ) {
            $message .= "\n* " . $connection->{ 'target' } . " " . $connection->{ 'details' }->{ 'sakai:state' };
	}
    }
    else {
        $message = "Problem viewing list!";
    }
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub list_rejected 
sub list_rejected {
    my ( $connection ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::list_rejected_setup( $connection->{ 'BaseURL' } ) );
    my $success = Sling::ConnectionUtil::list_rejected_eval( $res );
    my $message;
    if ( $success ) {
        my $all = from_json( $$res->content );
	$message = "Found \"" . $all->{ 'total' } . "\" connection(s):";
	foreach my $connection ( @{ $all->{ 'results' } } ) {
            $message .= "\n* " . $connection->{ 'target' } . " " . $connection->{ 'details' }->{ 'sakai:state' };
	}
    }
    else {
        $message = "Problem viewing list!";
    }
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub reject 
sub reject {
    my ( $connection, $reject ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::reject_setup( $connection->{ 'BaseURL' }, $reject ) );
    my $success = Sling::ConnectionUtil::reject_eval( $res );
    my $message = "Invitation: ";
    $message .= ( $success ? "rejected " : "was not rejected " ) . "for \"$reject\"";
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub remove 
sub remove {
    my ( $connection, $remove ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::remove_setup( $connection->{ 'BaseURL' }, $remove ) );
    my $success = Sling::ConnectionUtil::remove_eval( $res );
    my $message = "Invitation: ";
    $message .= ( $success ? "removed " : "was not removed " ) . "for \"$remove\"";
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

1;
