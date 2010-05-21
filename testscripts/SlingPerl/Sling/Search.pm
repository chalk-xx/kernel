#!/usr/bin/perl

package Sling::Search;

=head1 NAME

Search - search related functionality for Sakai implemented over rest
APIs.

=head1 ABSTRACT

Perl library providing a layer of abstraction to the REST search methods

=cut

#{{{imports
use strict;
use lib qw ( .. );
use JSON;
use Time::HiRes;
use Sling::Print;
use Sling::Request;
use Sling::SearchUtil;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a Search object.

=cut

sub new {
    my ( $class, $authn, $verbose, $log ) = @_;
    die "no authn provided!" unless defined $authn;
    my $response;
    my $search = { BaseURL => $$authn->{ 'BaseURL' },
                   Authn => $authn,
		   Hits => 0,
		   Message => "",
		   Response => \$response,
		   TimeElapse => 0,
		   Verbose => $verbose,
		   Log => $log };
    bless( $search, $class );
    return $search;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $search, $hits, $message, $response, $timeElapse ) = @_;
    $search->{ 'Hits' } = $hits;
    $search->{ 'Message' } = $message;
    $search->{ 'Response' } = $response;
    $search->{ 'TimeElapse' } = $timeElapse;
    return 1;
}
#}}}

#{{{sub search
sub search {
    my ( $search, $searchTerm, $page, $items ) = @_;
    my $startTime = Time::HiRes::time;
    my $res = Sling::Request::request( \$search,
        Sling::SearchUtil::search_setup( $search->{ 'BaseURL' }, $searchTerm, $page, $items ) );
    my $endTime = Time::HiRes::time;
    my $timeElapse = $endTime - $startTime;
    if ( Sling::SearchUtil::search_eval( $res ) ) {
        my $hits = from_json( $$res->content )->{ 'total' };
	# TODO make timeElapse significant to about 3 decimal places only in printed output.
	my $message = Sling::Print::dateTime .
	    " Searching for \"$searchTerm\": Search OK. Found $hits hit(s). Time $timeElapse seconds.";
        $search->set_results( $hits, $message, $res, $timeElapse );
	return $hits;
    }
    else {
        my $message = Sling::Print::dateTime . " Searching for \"$searchTerm\": Search failed!";
        $search->set_results( 0, $message, $res, $timeElapse );
	return 0;
    }
}
#}}}

#{{{sub search_sites
sub search_sites {
    my ( $search, $searchTerm, $page, $items ) = @_;
    my $startTime = Time::HiRes::time;
    my $res = Sling::Request::request( \$search,
        Sling::SearchUtil::search_sites_setup( $search->{ 'BaseURL' }, $searchTerm, $page, $items ) );
    my $endTime = Time::HiRes::time;
    my $timeElapse = $endTime - $startTime;
    if ( Sling::SearchUtil::search_sites_eval( $res ) ) {
        my $hits = from_json( $$res->content )->{ 'total' };
	# TODO make timeElapse significant to about 3 decimal places only in printed output.
	my $message = Sling::Print::dateTime .
	    " Searching for \"$searchTerm\": Search OK. Found $hits hit(s). Time $timeElapse seconds.";
	my $results = from_json( $$res->content )->{ 'results' };
        foreach my $result ( @{ $results } ) {
	    $message .= "\n" . $result->{ 'path' };
	}
        $search->set_results( $hits, $message, $res, $timeElapse );
	return $hits;
    }
    else {
        my $message = Sling::Print::dateTime . " Searching for \"$searchTerm\": Search failed!";
        $search->set_results( 0, $message, $res, $timeElapse );
	return 0;
    }
}
#}}}

#{{{sub search_users
sub search_users {
    my ( $search, $searchTerm, $page, $items ) = @_;
    my $startTime = Time::HiRes::time;
    my $res = Sling::Request::request( \$search,
        Sling::SearchUtil::search_users_setup( $search->{ 'BaseURL' }, $searchTerm, $page, $items ) );
    my $endTime = Time::HiRes::time;
    my $timeElapse = $endTime - $startTime;
    if ( Sling::SearchUtil::search_users_eval( $res ) ) {
        my $hits = from_json( $$res->content )->{ 'total' };
	# TODO make timeElapse significant to about 3 decimal places only in printed output.
	my $message = Sling::Print::dateTime .
	    " Searching for \"$searchTerm\": Search OK. Found $hits hit(s). Time $timeElapse seconds.";
	my $results = from_json( $$res->content )->{ 'results' };
        foreach my $result ( @{ $results } ) {
	    $message .= "\n" . $result->{ 'rep:userId' }[0];
	}
        $search->set_results( $hits, $message, $res, $timeElapse );
	return $hits;
    }
    else {
        my $message = Sling::Print::dateTime . " Searching for \"$searchTerm\": Search failed!";
        $search->set_results( 0, $message, $res, $timeElapse );
	return 0;
    }
}
#}}}

#{{{sub search_from_file
sub search_from_file {
    my ( $search, $file, $forkId, $numberForks ) = @_;
    $forkId = 0 unless defined $forkId;
    $numberForks = 1 unless defined $numberForks;
    my $count = 0;
    open ( FILE, $file );
    while ( <FILE> ) {
        if ( $forkId == ( $count++ % $numberForks ) ) {
            chomp;
	    $_ =~ /^(.*?)$/;
	    my $searchTerm = $1;
	    if ( $searchTerm !~ /^$/ ) {
                $search->search( $searchTerm );
		Sling::Print::print_result( $search );
	    }
	}
    }
    close ( FILE ); 
    return 1;
}
#}}}

1;
