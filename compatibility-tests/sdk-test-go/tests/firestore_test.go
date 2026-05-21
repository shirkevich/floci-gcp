package tests

import (
	"context"
	"testing"

	"floci-gcp-sdk-test-go/internal/testutil"

	"cloud.google.com/go/firestore"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/api/iterator"
)

func TestFirestore(t *testing.T) {
	ctx := context.Background()
	client := testutil.FirestoreClient(ctx)
	defer client.Close()

	colName := uniqueName("go-col")
	col := client.Collection(colName)

	t.Cleanup(func() {
		docs, _ := col.Documents(ctx).GetAll()
		for _, d := range docs {
			d.Ref.Delete(ctx)
		}
	})

	t.Run("SetAndGetDocument", func(t *testing.T) {
		docRef := col.Doc("alice")
		_, err := docRef.Set(ctx, map[string]interface{}{
			"name":   "Alice",
			"age":    30,
			"active": true,
		})
		require.NoError(t, err)

		snap, err := docRef.Get(ctx)
		require.NoError(t, err)
		assert.True(t, snap.Exists())

		name, err := snap.DataAt("name")
		require.NoError(t, err)
		assert.Equal(t, "Alice", name)

		age, err := snap.DataAt("age")
		require.NoError(t, err)
		assert.Equal(t, int64(30), age)

		_, err = docRef.Delete(ctx)
		require.NoError(t, err)
	})

	t.Run("QueryDocuments", func(t *testing.T) {
		col.Doc("user1").Set(ctx, map[string]interface{}{"name": "Alice", "score": 10})
		col.Doc("user2").Set(ctx, map[string]interface{}{"name": "Bob", "score": 20})
		col.Doc("user3").Set(ctx, map[string]interface{}{"name": "Charlie", "score": 10})

		t.Cleanup(func() {
			col.Doc("user1").Delete(ctx)
			col.Doc("user2").Delete(ctx)
			col.Doc("user3").Delete(ctx)
		})

		it := col.Where("score", "==", 10).Documents(ctx)
		var names []string
		for {
			snap, err := it.Next()
			if err == iterator.Done {
				break
			}
			require.NoError(t, err)
			name, _ := snap.DataAt("name")
			names = append(names, name.(string))
		}

		assert.Contains(t, names, "Alice")
		assert.Contains(t, names, "Charlie")
		assert.NotContains(t, names, "Bob")
	})

	t.Run("UpdateDocument", func(t *testing.T) {
		docRef := col.Doc("update-test")
		_, err := docRef.Set(ctx, map[string]interface{}{"name": "Alice", "age": 30})
		require.NoError(t, err)

		t.Cleanup(func() { docRef.Delete(ctx) })

		_, err = docRef.Update(ctx, []firestore.Update{
			{Path: "age", Value: 31},
		})
		require.NoError(t, err)

		snap, err := docRef.Get(ctx)
		require.NoError(t, err)
		age, _ := snap.DataAt("age")
		assert.Equal(t, int64(31), age)
		name, _ := snap.DataAt("name")
		assert.Equal(t, "Alice", name)
	})

	t.Run("DeleteDocument", func(t *testing.T) {
		docRef := col.Doc("to-delete")
		_, err := docRef.Set(ctx, map[string]interface{}{"name": "Delete Me"})
		require.NoError(t, err)

		_, err = docRef.Delete(ctx)
		require.NoError(t, err)

		snap, err := docRef.Get(ctx)
		assert.False(t, snap.Exists())
		_ = err
	})

	t.Run("BatchWrite", func(t *testing.T) {
		ref1 := col.Doc("batch1")
		ref2 := col.Doc("batch2")

		t.Cleanup(func() {
			ref1.Delete(ctx)
			ref2.Delete(ctx)
		})

		batch := client.Batch()
		batch.Set(ref1, map[string]interface{}{"name": "Batch1"})
		batch.Set(ref2, map[string]interface{}{"name": "Batch2"})
		_, err := batch.Commit(ctx)
		require.NoError(t, err)

		snap1, err := ref1.Get(ctx)
		require.NoError(t, err)
		n1, _ := snap1.DataAt("name")
		assert.Equal(t, "Batch1", n1)

		snap2, err := ref2.Get(ctx)
		require.NoError(t, err)
		n2, _ := snap2.DataAt("name")
		assert.Equal(t, "Batch2", n2)
	})
}
