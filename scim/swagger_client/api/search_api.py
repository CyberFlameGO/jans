# coding: utf-8

"""
    SCIM API

    Gluu SCIM 2.0 server API. Developers can think of SCIM as a REST API with endpoints exposing CRUD functionality (create, update, retrieve and delete) for identity management resources such as users, groups, and fido devices.   # noqa: E501

    OpenAPI spec version: 4.2.2
    
    Generated by: https://github.com/swagger-api/swagger-codegen.git
"""

from __future__ import absolute_import

import re  # noqa: F401

# python 2 and python 3 compatibility library
import six

from swagger_client.api_client import ApiClient


class SearchApi(object):
    """NOTE: This class is auto generated by the swagger code generator program.

    Do not edit the class manually.
    Ref: https://github.com/swagger-api/swagger-codegen
    """

    def __init__(self, api_client=None):
        if api_client is None:
            api_client = ApiClient()
        self.api_client = api_client

    def query_resource(self, body, **kwargs):  # noqa: E501
        """query_resource  # noqa: E501

        Search (from system root) for one or more resource (see section 3.4.3 of RFC 7644)   # noqa: E501
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please pass async_req=True
        >>> thread = api.query_resource(body, async_req=True)
        >>> result = thread.get()

        :param async_req bool
        :param SearchRequest body: Payload that represents the search criteria (required)
        :return: GenericListResponse
                 If the method is called asynchronously,
                 returns the request thread.
        """
        kwargs['_return_http_data_only'] = True
        if kwargs.get('async_req'):
            return self.query_resource_with_http_info(body, **kwargs)  # noqa: E501
        else:
            (data) = self.query_resource_with_http_info(body, **kwargs)  # noqa: E501
            return data

    def query_resource_with_http_info(self, body, **kwargs):  # noqa: E501
        """query_resource  # noqa: E501

        Search (from system root) for one or more resource (see section 3.4.3 of RFC 7644)   # noqa: E501
        This method makes a synchronous HTTP request by default. To make an
        asynchronous HTTP request, please pass async_req=True
        >>> thread = api.query_resource_with_http_info(body, async_req=True)
        >>> result = thread.get()

        :param async_req bool
        :param SearchRequest body: Payload that represents the search criteria (required)
        :return: GenericListResponse
                 If the method is called asynchronously,
                 returns the request thread.
        """

        all_params = ['body']  # noqa: E501
        all_params.append('async_req')
        all_params.append('_return_http_data_only')
        all_params.append('_preload_content')
        all_params.append('_request_timeout')

        params = locals()
        for key, val in six.iteritems(params['kwargs']):
            if key not in all_params:
                raise TypeError(
                    "Got an unexpected keyword argument '%s'"
                    " to method query_resource" % key
                )
            params[key] = val
        del params['kwargs']
        # verify the required parameter 'body' is set
        if ('body' not in params or
                params['body'] is None):
            raise ValueError("Missing the required parameter `body` when calling `query_resource`")  # noqa: E501

        collection_formats = {}

        path_params = {}

        query_params = []

        header_params = {}

        form_params = []
        local_var_files = {}

        body_params = None
        if 'body' in params:
            body_params = params['body']
        # HTTP header `Accept`
        header_params['Accept'] = self.api_client.select_header_accept(
            ['application/scim+json', 'application/json'])  # noqa: E501

        # HTTP header `Content-Type`
        header_params['Content-Type'] = self.api_client.select_header_content_type(  # noqa: E501
            ['application/scim+json', 'application/json'])  # noqa: E501

        # Authentication setting
        auth_settings = ['testmode']  # noqa: E501

        return self.api_client.call_api(
            '/jans-scim/restv1/v2/.search', 'POST',
            path_params,
            query_params,
            header_params,
            body=body_params,
            post_params=form_params,
            files=local_var_files,
            response_type='GenericListResponse',  # noqa: E501
            auth_settings=auth_settings,
            async_req=params.get('async_req'),
            _return_http_data_only=params.get('_return_http_data_only'),
            _preload_content=params.get('_preload_content', True),
            _request_timeout=params.get('_request_timeout'),
            collection_formats=collection_formats)
