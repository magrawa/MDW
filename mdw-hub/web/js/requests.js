// Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
'use strict';

var requestMod = angular.module('requests', ['mdw']);

requestMod.controller('RequestsController', ['$scope', '$http', '$location', 'mdw', 'util', 'REQUEST_STATUSES',
                                             function($scope, $http, $location, mdw, util, REQUEST_STATUSES) {
  

  // is this in the context of the Services tab
  $scope.context = $location.path().startsWith('/service') ? 'service' : 'workflow';
  $scope.defaultType = $scope.context == 'service' ? 'inboundRequests' : 'masterRequests';
  
  // two-way bound to/from directive
  $scope.requestList = {};
  $scope.requestFilter = { 
      status: '[Active]',
      sort: 'receivedDate',
      type: $scope.defaultType,
      descending: true
  }; 

  if ($scope.context == 'service') {
    $scope.requestTypes = {
        inboundRequests: 'Inbound', 
        outboundRequests: 'Outbound',
        masterRequests: 'Master Requests' 
      };
  }
  else {
    $scope.requestTypes = {
      masterRequests: 'Master Requests', 
      inboundRequests: 'Inbound', 
      outboundRequests: 'Outbound'
    };
  }
  
  // pseudo-status [Active] means non-final
  $scope.allStatuses = ['[Active]'].concat(REQUEST_STATUSES);  

  $scope.setRequestType = function(requestType) {
    $scope.typeaheadMatchSelection = null;
    $scope.clearTypeaheadFilters();
    $scope.requestFilter.type = requestType;
  };
  
  $scope.$on('page-retrieved', function(event, requestList) {
    // received date and end date, adjusted for db offset
    var dbDate = new Date(requestList.retrieveDate);
    requestList.requests.forEach(function(requestInstances) {
      requestInstances.receivedDate = util.formatDateTime(util.correctDbDate(new Date(requestInstances.receivedDate), dbDate));
      if (requestInstances.endDate)
        requestInstances.endDate = util.formatDateTime(util.correctDbDate(new Date(requestInstances.endDate), dbDate));
    });
    requestList.context = $scope.context;
  });   
  
  
  $scope.typeaheadMatchSelection = null;
  // docId or masterRequestId
  $scope.findTypeaheadMatches = function(typed) {
    var url = mdw.roots.services + '/services/Requests' + '?app=mdw-admin&type=' + $scope.requestFilter.type + '&find=' + typed;
    return $http.get(url).then(function(response) {
      // service returns matching requests
      var reqs = response.data.requests;
      var matches = [];
      reqs.forEach(function(req) {
        if ($scope.requestFilter.type == 'masterRequests')
          matches.push({value: req.masterRequestId});
        else
          matches.push({value: req.id});
      });
      return matches;
    });
  };

  $scope.clearTypeaheadFilters = function() {
    $scope.requestFilter.masterRequestId = null;
    $scope.requestFilter.id = null;
  };
  
  $scope.typeaheadChange = function() {
    if ($scope.typeaheadMatchSelection === null)
      $scope.clearTypeaheadFilters();
  };
  
  $scope.typeaheadSelect = function() {
    $scope.clearTypeaheadFilters();
    if ($scope.requestFilter.type == 'masterRequests') {
      $scope.requestFilter.masterRequestId = $scope.typeaheadMatchSelection.value;
    }
    else {
      $scope.requestFilter.id = $scope.typeaheadMatchSelection.value;
    }
  };
  
}]);

requestMod.controller('RequestController', ['$scope', '$location', '$route', '$routeParams', 'mdw', 'Request',
                                             function($scope, $location, $route, $routeParams, mdw, Request) {
  $scope.context = $location.path().startsWith('/service/') ? 'service' : 'workflow';
  
  var response = $route.current.loadedTemplateUrl == 'requests/response.html';
  var master = false;
  var id = $routeParams.requestId;
  var masterReqId = $routeParams.masterRequestId;
  if (masterReqId) {
    master = true;
    id = masterReqId;
  }
  $scope.request = Request.retrieve({requestId: id, master: master, response: response}, function() {
    var trimmed;
    if (response) {
      trimmed = $scope.request.responseContent.trim();
      if (trimmed.startsWith('{'))
        $scope.request.responseFormat = 'json';
      else if (trimmed.startsWith('<'))
        $scope.request.responseFormat = 'xml';
    }
    else {
      trimmed = $scope.request.content.trim();
      if (trimmed.startsWith('{'))
        $scope.request.format = 'json';
      else if (trimmed.startsWith('<'))
        $scope.request.format = 'xml';
    }
  });
}]);

requestMod.factory('Request', ['$resource', 'mdw', function($resource, mdw) {
  return $resource(mdw.roots.services + '/Services/Requests/:requestId', mdw.serviceParams(), {
    retrieve: { method: 'GET', isArray: false }
  });
}]);
        


