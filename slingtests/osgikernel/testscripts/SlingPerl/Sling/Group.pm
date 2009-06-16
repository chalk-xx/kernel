#!/usr/bin/perl

package Sling::Group;

=head1 NAME

Group - group related functionality for Sling implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST group methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use Text::CSV;
use Sling::GroupUtil;
use Sling::Print;
use Sling::Request;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Group Object.

=cut

sub new {
    my ( $class, $url, $lwpUserAgent ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $group = { BaseURL => "$url",
                 LWP => $lwpUserAgent,
		 Message => "",
		 Response => \$response };
    bless( $group, $class );
    return $group;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $group, $message, $response ) = @_;
    $group->{ 'Message' } = $message;
    $group->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub add
sub add {
    my ( $group, $actOnGroup, $properties, $log ) = @_;
    my $res = ${ $group->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::GroupUtil::add_setup( $group->{ 'BaseURL' },
	    $actOnGroup, $properties ), $group->{ 'LWP' } ) );
    my $success = Sling::GroupUtil::add_eval( \$res );
    my $message = "Group: \"$actOnGroup\" ";
    $message .= ( $success ? "added!" : "was not added!" );
    $group->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub delete
sub delete {
    my ( $group, $actOnGroup, $log ) = @_;
    my $res = ${ $group->{ 'LWP' } }->request( Sling::Request::string_to_request(
        Sling::GroupUtil::delete_setup( $group->{ 'BaseURL' }, $actOnGroup ), $group->{ 'LWP' } ) );
    my $success = Sling::GroupUtil::delete_eval( \$res );
    my $message = "Group: \"$actOnGroup\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $group->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub add_from_file
sub add_from_file {
    my ( $group, $file, $forkId, $numberForks, $log ) = @_;
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
		# First field must be site:
		if ( $column_headings[0] !~ /^group$/i ) {
		    die "First CSV column must be the group ID, ".
		        "column heading must be \"group\". ".
		        "Found: \"" . $column_headings[0] . "\".\n";
		}
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
		my $id = $columns[0];
		for ( my $i = 1; $i < $numberColumns ; $i++ ) {
                    my $value = $column_headings[ $i ] . "=" . $columns[ $i ];
		    push ( @properties, $value );
		}
                $group->add( $id, \@properties, $log );
		Sling::Print::print_lock( $group->{ 'Message' } ) if ( ! defined $log );
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

#{{{sub exists
sub exists {
    my ( $group, $actOnGroup, $log ) = @_;
    my $res = ${ $group->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::GroupUtil::exists_setup( $group->{ 'BaseURL' }, $actOnGroup ), $group->{ 'LWP' } ) );
    my $success = Sling::GroupUtil::exists_eval( \$res );
    my $message = "Group \"$actOnGroup\" ";
    $message .= ( $success ? "exists!" : "does not exist!" );
    $group->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

#{{{sub view
sub view {
    my ( $group, $actOnGroup, $log ) = @_;
    my $res = ${ $group->{ 'LWP' } }->request( Sling::Request::string_to_request(
                  Sling::GroupUtil::view_setup( $group->{ 'BaseURL' }, $actOnGroup ), $group->{ 'LWP' } ) );
    my $success = Sling::GroupUtil::view_eval( \$res );
    my $message = ( $success ? $res->content : "Problem viewing group: \"$actOnGroup\"" );
    $group->set_results( "$message", \$res );
    Sling::Print::print_file_lock( $message, $log ) if ( defined $log );
    return $success;
}
#}}}

1;
