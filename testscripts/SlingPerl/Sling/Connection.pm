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
use Text::CSV;
use Sling::Print;
use Sling::Request;
use Sling::ConnectionUtil;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Connection object.

=cut

sub new {
    my ( $class, $authn, $verbose, $log ) = @_;
    die "no authn provided!" unless defined $authn;
    my $response;
    my $connection = { BaseURL => $$authn->{ 'BaseURL' },
                       Authn => $authn,
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

#{{{sub cancel
sub cancel {
    my ( $connection, $cancel ) = @_;
    my $res = Sling::Request::request( \$connection,
        Sling::ConnectionUtil::cancel_setup( $connection->{ 'BaseURL' }, $cancel ) );
    my $success = Sling::ConnectionUtil::cancel_eval( $res );
    my $message = "Invitation: ";
    $message .= ( $success ? "cancelled " : "was not cancelled " ) . "for \"$cancel\"";
    $connection->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub connect_from_file
sub connect_from_file {
    my ( $connection, $file, $forkId, $numberForks ) = @_;
    my $csv = Text::CSV->new();
    my $count = 0;
    my $numberColumns = 0;
    my @column_headings;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $count++ == 0 ) {
	    # Parse file column headings first to determine field names:
	    if ( $csv->parse( $_ ) ) {
	        @column_headings = $csv->fields();
		my @expected_column_headings = ( "user", "password", "action", "contact", "types" );
		for ( my $i = 0 ; $i < @expected_column_headings; $i++ ) {
		    my $expected = $expected_column_headings[$i];
		    if ( $column_headings[$i] !~ /^$expected$/i ) {
		        die "CSV column $i expected to be \"" . $expected . "\"" .
		            "Found: \"" . $column_headings[$i] . "\".\n";
		    }
	        }
		# First field must be group:
		$numberColumns = @column_headings;
	    }
	    else {
	        die "CSV broken, failed to parse line: " . $csv->error_input;
	    }
	}
        elsif ( $forkId == ( $count++ % $numberForks ) ) {
	    my @properties;
	    if ( $csv->parse( $_ ) ) {
	        my @columns = $csv->fields();
		my $columns_size = @columns;
		# Check row has same number of columns as there were column headings:
		if ( $columns_size != $numberColumns ) {
		    die "Found \"$columns_size\" columns. There should have been \"$numberColumns\".\n".
		        "Row contents was: $_";
		}
		my $username = $columns[ 0 ];
		my $password = $columns[ 1 ];
		my $action = $columns[ 2 ];
		my $contact = $columns[ 3 ];
		my $connection_types = $columns[ 4 ];

                my $authn = $connection->{ 'Authn' };
                $$authn->switch_user( $username, $password );

		if ( $action =~ /^accept$/ ) {
                    $connection->accept( $contact );
		}
		elsif ( $action =~ /^block$/ ) {
                    $connection->block( $contact );
		}
		elsif ( $action =~ /^ignore$/ ) {
                    $connection->ignore( $contact );
		}
		elsif ( $action =~ /^invite$/ ) {
		    my @types = split ( /,/, $connection_types );
                    $connection->invite( $contact, \@types );
		}
		elsif ( $action =~ /^reject$/ ) {
                    $connection->reject( $contact );
		}
		elsif ( $action =~ /^remove$/ ) {
                    $connection->remove( $contact );
		}
		else {
		    die "Invalid action type: \"$action\".\n" .
		        "Row contents was: $_";
		}

		Sling::Print::print_result( $connection );
	    }
	    else {
	        die "CSV broken, failed to parse line: " . $csv->error_input;
	    }
	}
    }
    close ( FILE ); 
    return 1;
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
    my $authn = $connection->{ 'Authn' };
    my $username = $$authn->{ 'Username' };
    my $message = "Invitation: ";
    $message .= ( $success ? "issued " : "was not issued " ) .
        "to \"$invite\" by \"$username\".";
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
