#!/usr/bin/perl

package Shell::Site;

=head1 NAME

Shell::Site - provides site related functionality for the Sling Shell.

=head1 ABSTRACT

An administrative terminal for Sling, allowing interactive administration of the Sling system.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use vars qw(@ISA @EXPORT);
use Sling::Site;
use Term::ReadKey;
use Term::ANSIColor;
#}}}

@ISA = qw(Exporter);

require Exporter;

@EXPORT = qw(help_site_add smry_site_add help_site_exists smry_site_exists help_site_owners smry_site_owners help_site_add_owner smry_site_add_owner);

#{{{sub run_site_add
sub run_site_add {
    my ( $term, $id, $config ) = @_;
    if ( ! defined $id ) {
        $id = $term->prompt("Please enter a site id: ");
    }
    if ( $id =~ /^\s*$/ ) {
        return 1;
    }
    my $name = $term->prompt("Please enter a site name: ");
    if ( $name =~ /^\s*$/ ) {
        return 1;
    }
    my $description = $term->prompt("Please enter a site description: ");
    if ( $description =~ /^\s*$/ ) {
        return 1;
    }
    my $site = new Sling::Site( $config->{ 'host' }, $config->{ 'lwp' } );
    my $success = $site->add( $id, $name, $description );
    print $site->{ 'Message' } . "\n";
    if ( ! $success ) {
        print ${ $site->{ 'Response' } }->status_line . "\n";
    }
    return 1;
}
#}}}

#{{{sub help_site_add
sub help_site_add {
    return "Specify an id, name, and description to add a site to the system.";
}
#}}}

#{{{sub smry_site_add
sub smry_site_add {
    return "Add a new site to the system";
}
#}}}

#{{{sub run_site_add_owner
sub run_site_add_owner {
    my ( $term, $id, $config ) = @_;
    if ( ! defined $id ) {
        $id = $term->prompt("Please enter a site id: ");
    }
    if ( $id =~ /^\s*$/ ) {
        return 1;
    }
    my $owner = $term->prompt("Please enter the owner to be added: ");
    if ( $owner =~ /^\s*$/ ) {
        return 1;
    }
    my $site = new Sling::Site( $config->{ 'host' }, $config->{ 'lwp' } );
    my $success = $site->add_owner( $id, $owner );
    print $site->{ 'Message' } . "\n";
    if ( ! $success ) {
        print ${ $site->{ 'Response' } }->status_line . "\n";
    }
    return 1;
}
#}}}

#{{{sub help_site_add_owner
sub help_site_add_owner {
    return "Specify an id and owner to add an owner to that site in the system.";
}
#}}}

#{{{sub smry_site_add_owner
sub smry_site_add_owner {
    return "Add a new owner to a site in the system";
}
#}}}

#{{{sub run_site_exists
sub run_site_exists {
    my ( $term, $id, $config ) = @_;
    if ( ! defined $id ) {
        $id = $term->prompt("Please enter a site id: ");
    }
    if ( $id =~ /^\s*$/ ) {
        return 1;
    }
    my $site = new Sling::Site( $config->{ 'host' }, $config->{ 'lwp' } );
    $site->exists( $id );
    print $site->{ 'Message' } . "\n";
    return 1;
}
#}}}

#{{{sub help_site_exists
sub help_site_exists {
    return "Specify an id to check whether that site id exists in the system.";
}
#}}}

#{{{sub smry_site_exists
sub smry_site_exists {
    return "Check whether an site id exists in the system";
}
#}}}

#{{{sub run_site_owners
sub run_site_owners {
    my ( $term, $id, $config ) = @_;
    if ( ! defined $id ) {
        $id = $term->prompt("Please enter a site id: ");
    }
    if ( $id =~ /^\s*$/ ) {
        return 1;
    }
    my $site = new Sling::Site( $config->{ 'host' }, $config->{ 'lwp' } );
    $site->list_owners( $id );
    print $site->{ 'Message' } . "\n";
    return 1;
}
#}}}

#{{{sub help_site_owners
sub help_site_owners {
    return "Specify a site id to fetch a list of owners for that site.";
}
#}}}

#{{{sub smry_site_owners
sub smry_site_owners {
    return "Fetch list of owners of site.";
}
#}}}

1;
