package tests

import (
	"context"
	"fmt"
	"testing"

	"floci-gcp-sdk-test-go/internal/testutil"

	secretmanagerpb "cloud.google.com/go/secretmanager/apiv1/secretmanagerpb"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSecretManager(t *testing.T) {
	ctx := context.Background()
	client := testutil.SecretManagerClient(ctx)
	defer client.Close()

	parent := testutil.SecretParent()
	secretID := uniqueName("go-secret")
	secretName := fmt.Sprintf("%s/secrets/%s", parent, secretID)

	t.Cleanup(func() {
		client.DeleteSecret(ctx, &secretmanagerpb.DeleteSecretRequest{Name: secretName})
	})

	t.Run("CreateSecret", func(t *testing.T) {
		resp, err := client.CreateSecret(ctx, &secretmanagerpb.CreateSecretRequest{
			Parent:   parent,
			SecretId: secretID,
			Secret:   testutil.NewSecret(),
		})
		require.NoError(t, err)
		assert.Equal(t, secretName, resp.Name)
	})

	var versionName string

	t.Run("AddSecretVersion", func(t *testing.T) {
		resp, err := client.AddSecretVersion(ctx, &secretmanagerpb.AddSecretVersionRequest{
			Parent: secretName,
			Payload: &secretmanagerpb.SecretPayload{
				Data: []byte("super-secret-value"),
			},
		})
		require.NoError(t, err)
		versionName = resp.Name
		assert.NotEmpty(t, versionName)
	})

	t.Run("AccessSecretVersion", func(t *testing.T) {
		resp, err := client.AccessSecretVersion(ctx, &secretmanagerpb.AccessSecretVersionRequest{
			Name: secretName + "/versions/latest",
		})
		require.NoError(t, err)
		assert.Equal(t, []byte("super-secret-value"), resp.Payload.Data)
	})

	t.Run("AddSecondVersion", func(t *testing.T) {
		_, err := client.AddSecretVersion(ctx, &secretmanagerpb.AddSecretVersionRequest{
			Parent:  secretName,
			Payload: &secretmanagerpb.SecretPayload{Data: []byte("updated-value")},
		})
		require.NoError(t, err)
	})

	t.Run("ListSecretVersions", func(t *testing.T) {
		it := client.ListSecretVersions(ctx, &secretmanagerpb.ListSecretVersionsRequest{
			Parent: secretName,
		})
		count := 0
		for {
			_, err := it.Next()
			if err != nil {
				break
			}
			count++
		}
		assert.GreaterOrEqual(t, count, 2)
	})

	t.Run("ListSecrets", func(t *testing.T) {
		it := client.ListSecrets(ctx, &secretmanagerpb.ListSecretsRequest{Parent: parent})
		found := false
		for {
			s, err := it.Next()
			if err != nil {
				break
			}
			if s.Name == secretName {
				found = true
				break
			}
		}
		assert.True(t, found, "created secret should appear in list")
	})

	t.Run("DeleteSecret", func(t *testing.T) {
		err := client.DeleteSecret(ctx, &secretmanagerpb.DeleteSecretRequest{Name: secretName})
		require.NoError(t, err)

		it := client.ListSecrets(ctx, &secretmanagerpb.ListSecretsRequest{Parent: parent})
		for {
			s, err := it.Next()
			if err != nil {
				break
			}
			assert.NotEqual(t, secretName, s.Name)
		}
	})
}
