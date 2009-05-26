This package provides the base methods for path virtualization in Sling.

This is where a URL does not map directly onto a JCR location but undergoes some form or abstraction.
The classes in this package enable that to happen.

For instance:

If we want a URL to contain  folder with millions of items then JCR will not be able to support this, due
to its internal structure. Hence we can extend the VirtualResourceProvider as a Declaratively 
Managed Service to convert the URL into a hashed path into JCR

/users/ieb
becomes
/home/AB/CD/EF/ieb

Which *should* scale to about 16.7 million entries.

To use, extend the AbstractVirtualResourceProvider implementing the abstract methods. 
See other classes that extend this for examples.