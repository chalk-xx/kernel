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
use Fcntl ':flock';
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
    my ( $class, $url, $lwpUserAgent, $verbose ) = @_;
    die "url not defined!" unless defined $url;
    die "no lwp user agent provided!" unless defined $lwpUserAgent;
    my $response;
    my $search = { BaseURL => "$url",
                   LWP => $lwpUserAgent,
		   Hits => 0,
		   Message => "",
		   Response => \$response,
		   TimeElapse => 0,
		   Verbose => $verbose };
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
    my ( $search, $searchTerm, $log ) = @_;
    my $startTime = Time::HiRes::time;
    my $res = Sling::Request::request( \$search,
        Sling::SearchUtil::search_setup( $search->{ 'BaseURL' }, $searchTerm ) );
    my $endTime = Time::HiRes::time;
    my $timeElapse = $endTime - $startTime;
    if ( Sling::SearchUtil::search_eval( $res ) ) {
	my $hits = ($$res->content);
	$hits =~ s/.*?"total":([0-9]+).*/$1/;
	# Check hits total was correctly extracted:
	$hits = ( ( $hits =~ /^[0-9]+/ ) ? $hits : die "Problem calculating number of search hits!" );
	my $message = Sling::Print::dateTime .
	    " Searching for \"$searchTerm\": Search OK. Found $hits hits.";
	if ( defined $log && open( LOG, ">>$log" ) ) {
	    flock( LOG, LOCK_EX );
            print LOG $message;
	    printf LOG " Time: %.3f seconds.\n", $timeElapse;
            flock( LOG, LOCK_UN );
	    close( LOG );
	}
        $search->set_results( $hits, $message, $res, $timeElapse );
	return 1;
    }
    else {
        my $message = Sling::Print::dateTime . " Searching for \"$searchTerm\": Search failed!";
        $search->set_results( 0, $message, $res, $timeElapse );
	return 0;
    }
}
#}}}

#{{{sub search_all
sub search_all {
    my ( $search, $searchTerm, $log ) = @_;
    $search->search( $search, $searchTerm, "/", $log );
}
#}}}

#{{{sub search_user
sub search_user {
    my ( $search, $searchTerm, $log ) = @_;
    my $content = $search->search( $search, $searchTerm, "/_private", $log );
    my $firstName = $content;
    $firstName =~ s/.*"sakai:firstName":"([^"])*"/$1/;
    my $lastName = $content;
    $lastName =~ s/.*"sakai:lastName":"([^"])*"/$1/;
    print "FirstName: $firstName, LastName: $lastName\n";
}
#}}}

#{{{sub search_from_file
sub search_from_file {
    my ( $search, $file, $forkId, $numberForks, $log ) = @_;
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
                $search->search( $searchTerm, $log );
		Sling::Print::print_lock( $search->{ 'Message' } ) if ( ! defined $log );
	    }
	}
    }
    close ( FILE ); 
    return 1;
}
#}}}

1;
