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
use JSON;
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
    my ( $class, $authn, $verbose, $log ) = @_;
    die "no authn provided!" unless defined $authn;
    my $response;
    my $group = { BaseURL => $$authn->{ 'BaseURL' },
                  Authn => $authn,
		  Message => "",
		  Response => \$response,
		  Verbose => $verbose,
		  Log => $log };
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
    my ( $group, $actOnGroup, $properties ) = @_;
    my $res = Sling::Request::request( \$group,
        Sling::GroupUtil::add_setup( $group->{ 'BaseURL' }, $actOnGroup, $properties ) );
    my $success = Sling::GroupUtil::add_eval( $res );
    my $message = "Group: \"$actOnGroup\" ";
    $message .= ( $success ? "added!" : "was not added!" );
    $group->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub add_from_file
sub add_from_file {
    my ( $group, $file, $forkId, $numberForks ) = @_;
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
		# First field must be group:
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
                $group->add( $id, \@properties );
		Sling::Print::print_result( $group );
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

#{{{sub delete
sub delete {
    my ( $group, $actOnGroup ) = @_;
    my $res = Sling::Request::request( \$group,
        Sling::GroupUtil::delete_setup( $group->{ 'BaseURL' }, $actOnGroup ) );
    my $success = Sling::GroupUtil::delete_eval( $res );
    my $message = "Group: \"$actOnGroup\" ";
    $message .= ( $success ? "deleted!" : "was not deleted!" );
    $group->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub exists
sub exists {
    my ( $group, $actOnGroup ) = @_;
    my $res = Sling::Request::request( \$group,
        Sling::GroupUtil::exists_setup( $group->{ 'BaseURL' }, $actOnGroup ) );
    my $success = Sling::GroupUtil::exists_eval( $res );
    my $message = "Group \"$actOnGroup\" ";
    $message .= ( $success ? "exists!" : "does not exist!" );
    $group->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub member_add
sub member_add {
    my ( $group, $actOnGroup, $addMember ) = @_;
    my $res = Sling::Request::request( \$group,
        Sling::GroupUtil::member_add_setup( $group->{ 'BaseURL' }, $actOnGroup, $addMember ) );
    my $success = Sling::GroupUtil::member_add_eval( $res );
    my $message = "Member: \"$addMember\" ";
    $message .= ( $success ? "added" : "was not added" );
    $message .= " to group \"$actOnGroup\"!";
    $group->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub member_add_from_file
sub member_add_from_file {
    my ( $group, $file, $forkId, $numberForks ) = @_;
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
		# First field must be group:
		if ( $column_headings[0] !~ /^group$/i ) {
		    die "First CSV column must be the group ID, ".
		        "column heading must be \"group\". ".
		        "Found: \"" . $column_headings[0] . "\".\n";
		}
		# Second field must be user:
		if ( $column_headings[1] !~ /^user$/i ) {
		    die "Second CSV column must be the user ID, ".
		        "column heading must be \"user\". ".
		        "Found: \"" . $column_headings[1] . "\".\n";
		}
		$numberColumns = @column_headings;
	    }
	    else {
	        die "CSV broken, failed to parse line: " . $csv->error_input;
	    }
	}
        elsif ( $forkId == ( $count++ % $numberForks ) ) {
	    if ( $csv->parse( $_ ) ) {
	        my @columns = $csv->fields();
		my $columns_size = @columns;
		# Check row has same number of columns as there were column headings:
		if ( $columns_size != $numberColumns ) {
		    die "Found \"$columns_size\" columns. There should have been \"$numberColumns\".\n".
		        "Row contents was: $_";
		}
		my $actOnGroup = $columns[0];
		my $addMember = $columns[1];
                $group->member_add( $actOnGroup, $addMember );
		Sling::Print::print_result( $group );
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

#{{{sub member_delete
sub member_delete {
    my ( $group, $actOnGroup, $deleteMember ) = @_;
    my $res = Sling::Request::request( \$group,
        Sling::GroupUtil::member_delete_setup( $group->{ 'BaseURL' }, $actOnGroup, $deleteMember ) );
    my $success = Sling::GroupUtil::member_delete_eval( $res );
    my $message = "Member: \"$deleteMember\" ";
    $message .= ( $success ? "deleted" : "was not deleted" );
    $message .= " from group \"$actOnGroup\"!";
    $group->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub member_exists
sub member_exists {
    my ( $group, $actOnGroup, $existsMember ) = @_;
    my $res = Sling::Request::request( \$group,
        Sling::GroupUtil::view_setup( $group->{ 'BaseURL' }, $actOnGroup ) );
    my $success = Sling::GroupUtil::view_eval( $res );
    my $message;
    if ( $success ) {
        my $group_info = from_json( $$res->content );
	my $is_member = 0;
        foreach my $member ( @{ $group_info->{ 'members' } } ) {
            if ( $member =~ /^$existsMember$/ ) {
	        $is_member = 1;
		last;
	    }
        }
	$success = $is_member;
	$message = "\"$existsMember\" is " . ( $is_member ? "" : "not " ) .
	    "a member of group \"$actOnGroup\"";
    }
    else {
        $message = "Problem viewing group: \"$actOnGroup\"";
    }
    $group->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub member_view
sub member_view {
    my ( $group, $actOnGroup ) = @_;
    my $res = Sling::Request::request( \$group,
        Sling::GroupUtil::view_setup( $group->{ 'BaseURL' }, $actOnGroup ) );
    my $success = Sling::GroupUtil::view_eval( $res );
    my $message;
    if ( $success ) {
        my $group_info = from_json( $$res->content );
        my $number_members = @{ $group_info->{ 'members' } };
        my $members = "Group \"$actOnGroup\" has $number_members member(s):";
        foreach my $member ( @{ $group_info->{ 'members' } } ) {
            $members .= "\n$member";
        }
	$message = "$members";
	$success = $number_members;
    }
    else {
        $message = "Problem viewing group: \"$actOnGroup\"";
    }
    $group->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub view
sub view {
    my ( $group, $actOnGroup ) = @_;
    my $res = Sling::Request::request( \$group,
        Sling::GroupUtil::view_setup( $group->{ 'BaseURL' }, $actOnGroup ) );
    my $success = Sling::GroupUtil::view_eval( $res );
    my $message = ( $success ? $$res->content : "Problem viewing group: \"$actOnGroup\"" );
    $group->set_results( "$message", $res );
    return $success;
}
#}}}

1;
