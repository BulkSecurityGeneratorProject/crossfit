'use strict';

angular.module('crossfitApp').controller('MembershipTypeDialogController',
    ['$scope', '$stateParams', '$modalInstance', 'entity', 'MembershipType', 'CrossFitBox',
        function($scope, $stateParams, $modalInstance, entity, MembershipType, CrossFitBox) {

        $scope.membershipType = entity;
        $scope.crossfitboxs = CrossFitBox.query();
        $scope.load = function(id) {
            MembershipType.get({id : id}, function(result) {
                $scope.membershipType = result;
            });
        };

        var onSaveFinished = function (result) {
            $scope.$emit('crossfitApp:membershipTypeUpdate', result);
            $modalInstance.close(result);
        };

        $scope.save = function () {
            if ($scope.membershipType.id != null) {
                MembershipType.update($scope.membershipType, onSaveFinished);
            } else {
                MembershipType.save($scope.membershipType, onSaveFinished);
            }
        };

        $scope.clear = function() {
            $modalInstance.dismiss('cancel');
        };
}]);
