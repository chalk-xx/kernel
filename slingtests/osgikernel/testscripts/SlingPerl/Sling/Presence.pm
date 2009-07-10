#!/usr/bin/perl

package Sling::Presence;

=head1 NAME

Presence - presence related functionality for Sakai implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST presence methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use JSON;
use Text::CSV;
use Sling::Print;
use Sling::Request;
use Sling::PresenceUtil;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Presence object.

=cut

sub new {
    my ( $class, $authn, $verbose, $log ) = @_;
    die "no authn provided!" unless defined $authn;
    my $response;
    my $presence = { BaseURL => $$authn->{ 'BaseURL' },
                     Authn => $authn,
		     Message => "",
		     Response => \$response,
		     Verbose => $verbose,
		     Log => $log };
    bless( $presence, $class );
    return $presence;
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

#{{{sub contacts
sub contacts {
    my ( $presence ) = @_;
    my $res = Sling::Request::request( \$presence,
        Sling::PresenceUtil::contacts_setup( $presence->{ 'BaseURL' } ) );
    my $success = Sling::PresenceUtil::contacts_eval( $res );
    my $message;
    if ( $success ) {
        my $contacts_status = from_json( $$res->content )->{ 'contacts' };
	my $contacts_count = @{ $contacts_status };
        $message = "Contacts presence status available for $contacts_count contact(s):";
	foreach my $contact_status ( @{ $contacts_status } ) {
	    $message .= "\n$contact_status";
	}
	$success = $contacts_count;
    }
    else {
        $message = "Problem viewing contacts presence status!";
    }
    $presence->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub delete
sub delete {
    my ( $presence ) = @_;
    my $res = Sling::Request::request( \$presence,
        Sling::PresenceUtil::delete_setup( $presence->{ 'BaseURL' } ) );
    my $success = Sling::PresenceUtil::delete_eval( $res );
    my $message = "Presence status was ";
    $message .= ( $success ? "" : "not " ) . "successfully cleared!";
    $presence->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub status
sub status {
    my ( $presence ) = @_;
    my $res = Sling::Request::request( \$presence,
        Sling::PresenceUtil::status_setup( $presence->{ 'BaseURL' } ) );
    my $success = Sling::PresenceUtil::status_eval( $res );
    my $message .= ( $success ? $$res->content : "Problem viewing presence status!" );
    $presence->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub update
sub update {
    my ( $presence, $location, $status ) = @_;
    my $res = Sling::Request::request( \$presence,
        Sling::PresenceUtil::update_setup( $presence->{ 'BaseURL' }, $location, $status ) );
    my $success = Sling::PresenceUtil::update_eval( $res );
    my $message = "Presence status was ";
    $message .= ( $success ? "" : "not " ) . "successfully updated!";
    $presence->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub update_from_file
sub update_from_file {
    my ( $presence, $file, $forkId, $numberForks ) = @_;
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
		my @expected_column_headings = ( "user", "password", "action", "location", "status" );
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
		my $location = $columns[ 3 ];
		my $status = $columns[ 4 ];

                my $authn = $presence->{ 'Authn' };
                $$authn->switch_user( $username, $password );

		if ( $action =~ /^update$/ ) {
                    $presence->update( $location, $status );
		}
		elsif ( $action =~ /^delete$/ ) {
                    $presence->delete();
		}
		else {
		    die "Invalid action type: \"$action\".\n" .
		        "Row contents was: $_";
		}

		Sling::Print::print_result( $presence );
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


1;
